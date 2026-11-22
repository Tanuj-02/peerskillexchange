package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanuj.peerskillexchange.model.*;
import com.tanuj.peerskillexchange.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.http.HttpStatus.*;

@Service
public class AiSessionService {
    private final LearningRoomService rooms;
    private final LearningRoomRepository roomRepository;
    private final AiSessionJobRepository jobs;
    private final AiSessionReportRepository reports;
    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final ExchangeRequestRepository exchanges;
    private final ObjectMapper json;
    private final VideoCallService calls;
    private final VideoCallNoteRepository callNotes;

    public AiSessionService(LearningRoomService rooms, LearningRoomRepository roomRepository,
            AiSessionJobRepository jobs, AiSessionReportRepository reports,
            ConversationRepository conversations, ChatMessageRepository messages,
            ExchangeRequestRepository exchanges, ObjectMapper json,
            VideoCallService calls, VideoCallNoteRepository callNotes) {
        this.rooms = rooms; this.roomRepository = roomRepository; this.jobs = jobs;
        this.reports = reports; this.conversations = conversations; this.messages = messages;
        this.exchanges = exchanges; this.json = json;
        this.calls = calls; this.callNotes = callNotes;
    }

    public record JobResponse(Long id, String status, String summary, List<String> questions,
                              String errorCode, int attempts, LocalDateTime createdAt, LocalDateTime completedAt) {}

    public JobResponse response(AiSessionJob job) {
        List<String> questions = List.of();
        if (job.getStatus() == AiSessionJob.Status.SUCCEEDED && job.getQuestionsJson() != null) {
            try { questions = json.readValue(job.getQuestionsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}); }
            catch (JsonProcessingException e) { throw new IllegalStateException("Stored AI result is invalid."); }
        }
        return new JobResponse(job.getId(), job.getStatus().name(), job.getSummary(), questions,
                job.getErrorCode(), job.getAttempts(), job.getCreatedAt(), job.getCompletedAt());
    }

    @Transactional
    public JobResponse request(String username, Long roomId, String requestId) {
        if (requestId == null || !requestId.matches("[0-9a-fA-F-]{36}")) {
            throw new ResponseStatusException(BAD_REQUEST, "A UUID request ID is required.");
        }
        try { UUID.fromString(requestId); } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid request ID.");
        }
        User me = rooms.currentUser(username);
        LearningRoom room = roomRepository.lockById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Room not found."));
        rooms.assertMember(me, room);
        if (room.getStatus() != LearningRoom.RoomStatus.CLOSED) {
            throw new ResponseStatusException(CONFLICT, "Close the room before requesting AI notes.");
        }
        var previous = jobs.findByRoomIdAndRequestId(roomId, requestId);
        if (previous.isPresent()) {
            if (!previous.get().getRequestedBy().equals(me.getId())) throw new AccessDeniedException("Request ID belongs to another member.");
            return response(previous.get());
        }
        if (jobs.countByRoomId(roomId) >= 3) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "This room has reached its AI generation limit.");
        }
        var latest = jobs.findFirstByRoomIdOrderByIdDesc(roomId);
        if (latest.isPresent() && (latest.get().getStatus() == AiSessionJob.Status.QUEUED
                || latest.get().getStatus() == AiSessionJob.Status.RUNNING)) {
            throw new ResponseStatusException(CONFLICT, "An AI request is already in progress.");
        }
        AiSessionJob job = new AiSessionJob();
        job.setRoom(room); job.setRequestId(requestId); job.setRequestedBy(me.getId());
        return response(jobs.saveAndFlush(job));
    }

    @Transactional(readOnly = true)
    public JobResponse latest(String username, Long roomId) {
        rooms.requireMemberRoom(username, roomId);
        return jobs.findFirstByRoomIdOrderByIdDesc(roomId).map(this::response).orElse(null);
    }

    @Transactional
    public JobResponse requestForCall(String username, Long roomId, Long callId, String requestId) {
        if (requestId == null || !requestId.matches("[0-9a-fA-F-]{36}")) {
            throw new ResponseStatusException(BAD_REQUEST, "A UUID request ID is required.");
        }
        try { UUID.fromString(requestId); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(BAD_REQUEST, "Invalid request ID."); }
        roomRepository.lockById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Room not found."));
        VideoCall call = calls.requireCall(username, roomId, callId, false);
        if (call.getEndedAt() == null) throw new ResponseStatusException(CONFLICT, "End the call before requesting its summary.");
        User me = rooms.currentUser(username);
        var previous = jobs.findByRoomIdAndRequestId(roomId, requestId);
        if (previous.isPresent()) {
            if (!previous.get().getRequestedBy().equals(me.getId())
                    || previous.get().getCall() == null || !previous.get().getCall().getId().equals(callId)) {
                throw new AccessDeniedException("Request ID belongs to another summary.");
            }
            return response(previous.get());
        }
        if (callNotes.findTop40ByCallIdOrderByIdAsc(callId).isEmpty()) {
            throw new ResponseStatusException(CONFLICT, "Add call notes before requesting a summary.");
        }
        if (jobs.countByCallId(callId) >= 3) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "This call has reached its AI generation limit.");
        }
        var latest = jobs.findFirstByCallIdOrderByIdDesc(callId);
        if (latest.isPresent() && (latest.get().getStatus() == AiSessionJob.Status.QUEUED
                || latest.get().getStatus() == AiSessionJob.Status.RUNNING)) {
            throw new ResponseStatusException(CONFLICT, "A summary is already in progress.");
        }
        AiSessionJob job = new AiSessionJob();
        job.setRoom(call.getRoom()); job.setCall(call); job.setRequestId(requestId); job.setRequestedBy(me.getId());
        return response(jobs.saveAndFlush(job));
    }

    @Transactional(readOnly = true)
    public JobResponse latestForCall(String username, Long roomId, Long callId) {
        calls.requireCall(username, roomId, callId, false);
        return jobs.findFirstByCallIdOrderByIdDesc(callId).map(this::response).orElse(null);
    }

    @Transactional
    public void report(String username, Long roomId, Long jobId, String reason) {
        User me = rooms.currentUser(username);
        rooms.requireMemberRoom(username, roomId);
        if (!Set.of("INCORRECT", "UNHELPFUL", "INAPPROPRIATE").contains(reason)) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose a valid report reason.");
        }
        AiSessionJob job = jobs.findById(jobId).filter(j -> j.getRoom().getId().equals(roomId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "AI result not found."));
        if (job.getStatus() != AiSessionJob.Status.SUCCEEDED) {
            throw new ResponseStatusException(CONFLICT, "Only completed results can be reported.");
        }
        if (reports.existsByJobIdAndReporterId(jobId, me.getId())) return;
        AiSessionReport report = new AiSessionReport();
        report.setJob(job); report.setReporterId(me.getId()); report.setReason(reason);
        reports.save(report);
    }

    @Transactional(readOnly = true)
    public String context(Long jobId) {
        AiSessionJob job = jobs.findById(jobId).orElseThrow();
        LearningRoom room = job.getRoom();
        ExchangeRequest exchange = exchanges.findById(room.getExchangeRequestId()).orElseThrow();
        StringBuilder context = new StringBuilder();
        context.append("Offered skills: ");
        exchange.getOfferedSkills().stream().limit(8).forEach(s -> context.append(s.getSkillname(), 0, Math.min(80, s.getSkillname().length())).append(", "));
        context.append("\nRequested skills: ");
        exchange.getAskingSkills().stream().limit(8).forEach(s -> context.append(s.getSkillname(), 0, Math.min(80, s.getSkillname().length())).append(", "));
        if (job.getCall() != null) {
            context.append("\nSummarize only this video call. These are member-provided notes or opt-in speech captions, not a complete recording. Do not invent missing dialogue.\nCall notes (untrusted):\n");
            callNotes.findTop40ByCallIdOrderByIdAsc(job.getCall().getId()).forEach(note -> {
                if (context.length() < 3800) context.append("- ")
                        .append(note.getContent().replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[email redacted]"))
                        .append('\n');
            });
            return context.substring(0, Math.min(4000, context.length()));
        }
        context.append("\nSession chat excerpts (untrusted):\n");
        User owner = room.getOwner(); User peer = room.getPeer();
        LocalDateTime contextEnd = room.getClosedAt() == null || room.getClosedAt().isAfter(job.getCreatedAt())
                ? job.getCreatedAt() : room.getClosedAt();
        conversations.findByUser1AndUser2(owner, peer)
                .or(() -> conversations.findByUser1AndUser2(peer, owner))
                .ifPresent(conversation -> messages.findForRoom(conversation, room.getCreatedAt(),
                                contextEnd, PageRequest.of(0, 20))
                        .getContent().stream()
                        .sorted(Comparator.comparing(ChatMessage::getId))
                        .forEach(message -> {
                            if (context.length() < 3800) context.append("- ")
                                    .append(message.getContent().substring(0, Math.min(message.getContent().length(), 200))
                                            .replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[email redacted]"))
                                    .append('\n');
                        }));
        return context.substring(0, Math.min(4000, context.length()));
    }

    @Transactional
    public boolean claim(Long jobId) {
        AiSessionJob job = jobs.lockById(jobId).orElseThrow();
        if (job.getStatus() == AiSessionJob.Status.SUCCEEDED || job.getStatus() == AiSessionJob.Status.FAILED) return false;
        job.setStatus(AiSessionJob.Status.RUNNING);
        job.setAttempts(job.getAttempts() + 1);
        return true;
    }

    @Transactional
    public void succeeded(Long jobId, AiSessionGenerator.Result result) {
        AiSessionJob job = jobs.lockById(jobId).orElseThrow();
        if (job.getStatus() != AiSessionJob.Status.RUNNING) return;
        try { job.setQuestionsJson(json.writeValueAsString(result.questions())); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
        job.setSummary(result.summary()); job.setStatus(AiSessionJob.Status.SUCCEEDED);
        job.setErrorCode(null); job.setCompletedAt(LocalDateTime.now());
    }

    @Transactional
    public boolean failedAttempt(Long jobId, String code) {
        AiSessionJob job = jobs.lockById(jobId).orElseThrow();
        if (job.getStatus() != AiSessionJob.Status.RUNNING) return false;
        if (job.getAttempts() >= 3 || code.equals("not_configured") || code.equals("invalid_output")) {
            job.setStatus(AiSessionJob.Status.FAILED); job.setErrorCode(code); job.setCompletedAt(LocalDateTime.now());
            return false;
        }
        return true;
    }
}
