package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.board.RoomCursor;
import com.tanuj.peerskillexchange.dto.board.RoomEvent;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.UserRepository;
import com.tanuj.peerskillexchange.service.LearningRoomService;
import com.tanuj.peerskillexchange.service.RoomPresenceService;
import com.tanuj.peerskillexchange.service.VideoCallService;
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
    private final VideoCallService calls;

    public RoomWsController(LearningRoomService rooms, UserRepository users,
                            SimpMessagingTemplate messaging, RoomPresenceService presence,
                            VideoCallService calls) {
        this.rooms = rooms;
        this.users = users;
        this.messaging = messaging;
        this.presence = presence;
        this.calls = calls;
    }

    public record CursorInput(double x, double y, boolean active) {}
    public record SignalInput(Long callId, String kind, String targetUserId, String payload) {}
    public record SignalEvent(String type, Long callId, String kind, String fromUserId,
                              String targetUserId, String payload) {}

    @MessageMapping("/rooms.signal/{roomId}")
    public void signal(@DestinationVariable Long roomId, SignalInput input, Principal principal) {
        if (principal == null) throw new AccessDeniedException("Authentication required.");
        if (input == null || input.callId() == null || input.kind() == null
                || !java.util.Set.of("READY", "OFFER", "ANSWER", "ICE", "LEAVE").contains(input.kind())
                || input.payload() != null && input.payload().length() > 20000) {
            throw new IllegalArgumentException("Invalid call signal.");
        }
        var call = calls.requireCall(principal.getName(), roomId, input.callId(), true);
        User user = users.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Account unavailable."));
        String peerId = call.getRoom().getOwner().getId().equals(user.getId())
                ? call.getRoom().getPeer().getId() : call.getRoom().getOwner().getId();
        if (!peerId.equals(input.targetUserId())) throw new AccessDeniedException("Call target denied.");
        if ((input.kind().equals("OFFER") || input.kind().equals("ANSWER") || input.kind().equals("ICE"))
                && (input.payload() == null || input.payload().isBlank())) {
            throw new IllegalArgumentException("Signal payload required.");
        }
        messaging.convertAndSend("/topic/rooms/" + roomId,
                new SignalEvent("SIGNAL", input.callId(), input.kind(), user.getId(), peerId, input.payload()));
    }

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
