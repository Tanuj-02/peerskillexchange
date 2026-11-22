package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.service.VideoCallService;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/calls")
public class VideoCallController {
    private final VideoCallService calls;
    private final SimpMessagingTemplate messaging;
    public VideoCallController(VideoCallService calls, SimpMessagingTemplate messaging) {
        this.calls = calls; this.messaging = messaging;
    }
    private String username() { return SecurityContextHolder.getContext().getAuthentication().getName(); }
    public record NoteInput(String requestId, String content) {}

    @GetMapping
    public List<VideoCallService.CallResponse> list(@PathVariable Long roomId) {
        return calls.list(username(), roomId);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public VideoCallService.CallResponse start(@PathVariable Long roomId) {
        var call = calls.start(username(), roomId);
        messaging.convertAndSend("/topic/rooms/" + roomId, new CallEvent("CALL_STARTED", call));
        return call;
    }
    @PostMapping("/{callId}/end")
    public VideoCallService.CallResponse end(@PathVariable Long roomId, @PathVariable Long callId) {
        var call = calls.end(username(), roomId, callId);
        messaging.convertAndSend("/topic/rooms/" + roomId, new CallEvent("CALL_ENDED", call));
        return call;
    }
    @GetMapping("/{callId}/notes")
    public List<VideoCallService.NoteResponse> notes(@PathVariable Long roomId, @PathVariable Long callId) {
        return calls.notes(username(), roomId, callId);
    }
    @PostMapping("/{callId}/notes") @ResponseStatus(HttpStatus.CREATED)
    public VideoCallService.NoteResponse note(@PathVariable Long roomId, @PathVariable Long callId,
                                             @RequestBody NoteInput input) {
        return calls.addNote(username(), roomId, callId, input.requestId(), input.content());
    }
    public record CallEvent(String type, VideoCallService.CallResponse call) {}
}
