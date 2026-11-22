package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.service.AiSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{roomId}/ai")
public class AiSessionController {
    private final AiSessionService service;
    public AiSessionController(AiSessionService service) { this.service = service; }
    private String username() { return SecurityContextHolder.getContext().getAuthentication().getName(); }
    public record Request(String requestId) {}
    public record Report(String reason) {}
    @PostMapping("/jobs") @ResponseStatus(HttpStatus.ACCEPTED)
    public AiSessionService.JobResponse request(@PathVariable Long roomId, @RequestBody Request input) {
        return service.request(username(), roomId, input.requestId());
    }
    @GetMapping("/jobs/latest")
    public AiSessionService.JobResponse latest(@PathVariable Long roomId) {
        return service.latest(username(), roomId);
    }
    @PostMapping("/jobs/{jobId}/reports") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@PathVariable Long roomId, @PathVariable Long jobId, @RequestBody Report input) {
        service.report(username(), roomId, jobId, input.reason());
    }
}
