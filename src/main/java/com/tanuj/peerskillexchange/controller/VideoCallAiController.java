package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.service.AiSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{roomId}/calls/{callId}/ai")
public class VideoCallAiController {
    private final AiSessionService ai;
    public VideoCallAiController(AiSessionService ai) { this.ai = ai; }
    private String username() { return SecurityContextHolder.getContext().getAuthentication().getName(); }
    public record Request(String requestId) {}
    @PostMapping("/jobs") @ResponseStatus(HttpStatus.ACCEPTED)
    public AiSessionService.JobResponse request(@PathVariable Long roomId, @PathVariable Long callId,
                                                @RequestBody Request input) {
        return ai.requestForCall(username(), roomId, callId, input.requestId());
    }
    @GetMapping("/jobs/latest")
    public AiSessionService.JobResponse latest(@PathVariable Long roomId, @PathVariable Long callId) {
        return ai.latestForCall(username(), roomId, callId);
    }
}
