package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.notification.NotificationResponse;
import com.tanuj.peerskillexchange.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by("createdAt").descending()
        );
        return notificationService.listForCurrentUser(pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(notificationService.unreadCountForCurrentUser());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/read-all")
    public MarkAllReadResponse markAllRead() {
        int updated = notificationService.markAllReadForCurrentUser();
        return new MarkAllReadResponse(updated);
    }

    public record UnreadCountResponse(long unreadCount) {}

    public record MarkAllReadResponse(int updated) {}
}
