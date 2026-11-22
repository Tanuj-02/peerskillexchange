package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.model.LearningRoom;
import com.tanuj.peerskillexchange.model.VideoCall;
import com.tanuj.peerskillexchange.model.VideoCallNote;
import com.tanuj.peerskillexchange.repository.VideoCallNoteRepository;
import com.tanuj.peerskillexchange.repository.VideoCallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
public class VideoCallService {
    private final VideoCallRepository calls;
    private final VideoCallNoteRepository notes;
    private final LearningRoomService rooms;

    public VideoCallService(VideoCallRepository calls, VideoCallNoteRepository notes, LearningRoomService rooms) {
        this.calls = calls; this.notes = notes; this.rooms = rooms;
    }

    public record CallResponse(Long id, Long roomId, String startedBy, LocalDateTime startedAt,
                               LocalDateTime endedAt) {}
    public record NoteResponse(Long id, String authorId, String content, LocalDateTime createdAt) {}

    private CallResponse response(VideoCall call) {
        return new CallResponse(call.getId(), call.getRoom().getId(), call.getStartedBy(),
                call.getStartedAt(), call.getEndedAt());
    }

    @Transactional
    public CallResponse start(String username, Long roomId) {
        LearningRoom room = calls.lockRoom(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Room not found."));
        var me = rooms.currentUser(username);
        rooms.assertOpenMember(me, room);
        var active = calls.findFirstByRoomIdAndEndedAtIsNullOrderByStartedAtDesc(roomId);
        if (active.isPresent()) return response(active.get());
        VideoCall call = new VideoCall();
        call.setRoom(room); call.setStartedBy(me.getId());
        return response(calls.saveAndFlush(call));
    }

    @Transactional(readOnly = true)
    public List<CallResponse> list(String username, Long roomId) {
        rooms.requireMemberRoom(username, roomId);
        return calls.findByRoomIdOrderByStartedAtDesc(roomId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public VideoCall requireCall(String username, Long roomId, Long callId, boolean active) {
        rooms.requireMemberRoom(username, roomId);
        VideoCall call = calls.findById(callId)
                .filter(found -> found.getRoom().getId().equals(roomId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Call not found."));
        if (active && call.getEndedAt() != null) {
            throw new ResponseStatusException(CONFLICT, "Call has ended.");
        }
        return call;
    }

    @Transactional
    public CallResponse end(String username, Long roomId, Long callId) {
        VideoCall call = requireCall(username, roomId, callId, false);
        if (call.getEndedAt() == null) call.setEndedAt(LocalDateTime.now());
        return response(call);
    }

    @Transactional
    public NoteResponse addNote(String username, Long roomId, Long callId, String requestId, String content) {
        VideoCall call = requireCall(username, roomId, callId, true);
        String authorId = rooms.currentUser(username).getId();
        try { UUID.fromString(requestId); }
        catch (RuntimeException e) { throw new ResponseStatusException(BAD_REQUEST, "A valid note ID is required."); }
        if (content == null || content.isBlank() || content.length() > 1000) {
            throw new ResponseStatusException(BAD_REQUEST, "Note must contain 1 to 1000 characters.");
        }
        if (notes.existsByCallIdAndAuthorIdAndRequestId(callId, authorId, requestId)) {
            throw new ResponseStatusException(CONFLICT, "Note already saved.");
        }
        VideoCallNote note = new VideoCallNote();
        note.setCall(call); note.setAuthorId(authorId); note.setRequestId(requestId);
        note.setContent(content.trim());
        note = notes.saveAndFlush(note);
        return new NoteResponse(note.getId(), note.getAuthorId(), note.getContent(), note.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> notes(String username, Long roomId, Long callId) {
        requireCall(username, roomId, callId, false);
        return notes.findTop40ByCallIdOrderByIdAsc(callId).stream()
                .map(note -> new NoteResponse(note.getId(), note.getAuthorId(), note.getContent(), note.getCreatedAt()))
                .toList();
    }
}
