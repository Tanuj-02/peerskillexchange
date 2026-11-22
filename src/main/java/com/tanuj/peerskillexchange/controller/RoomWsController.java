package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.board.RoomCursor;
import com.tanuj.peerskillexchange.dto.board.RoomEvent;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.UserRepository;
import com.tanuj.peerskillexchange.service.LearningRoomService;
import com.tanuj.peerskillexchange.service.RoomPresenceService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class RoomWsController {
    private final LearningRoomService rooms;
    private final UserRepository users;
    private final SimpMessagingTemplate messaging;
    private final RoomPresenceService presence;

    public RoomWsController(LearningRoomService rooms, UserRepository users,
                            SimpMessagingTemplate messaging, RoomPresenceService presence) {
        this.rooms = rooms;
        this.users = users;
        this.messaging = messaging;
        this.presence = presence;
    }

    public record CursorInput(double x, double y, boolean active) {}

    @MessageMapping("/rooms.cursor/{roomId}")
    public void cursor(@DestinationVariable Long roomId, CursorInput input, Principal principal) {
        if (principal == null) throw new AccessDeniedException("Authentication required.");
        rooms.assertAccess(principal.getName(), roomId);
        if (input == null || !Double.isFinite(input.x()) || !Double.isFinite(input.y())
                || input.x() < 0 || input.x() > 1 || input.y() < 0 || input.y() > 1) {
            throw new IllegalArgumentException("Invalid cursor position.");
        }
        User user = users.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Account unavailable."));
        presence.touch(roomId, user.getId());
        messaging.convertAndSend("/topic/rooms/" + roomId,
                RoomEvent.cursor(new RoomCursor(user.getId(), user.getName(),
                        input.x(), input.y(), input.active(), LocalDateTime.now())));
    }
}
