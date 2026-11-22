package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.model.LearningRoom;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.model.VideoCall;
import com.tanuj.peerskillexchange.repository.UserRepository;
import com.tanuj.peerskillexchange.service.LearningRoomService;
import com.tanuj.peerskillexchange.service.RoomPresenceService;
import com.tanuj.peerskillexchange.service.VideoCallService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomWsControllerTest {
    @Test
    void callSignalsOnlyReachTheOtherMemberOfAnActiveCall() {
        var rooms = mock(LearningRoomService.class);
        var calls = mock(VideoCallService.class);
        var users = mock(UserRepository.class);
        var messages = mock(SimpMessagingTemplate.class);
        var controller = new RoomWsController(rooms, users, messages, mock(RoomPresenceService.class), calls);
        User alice = new User(); alice.setId("alice-id");
        User bob = new User(); bob.setId("bob-id");
        LearningRoom room = new LearningRoom(); room.setOwner(alice); room.setPeer(bob);
        VideoCall call = new VideoCall(); call.setRoom(room);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(calls.requireCall("alice", 42L, 7L, true)).thenReturn(call);
        controller.signal(42L, new RoomWsController.SignalInput(7L, "READY", "bob-id", null), () -> "alice");
        verify(messages).convertAndSend(eq("/topic/rooms/42"),
                eq(new RoomWsController.SignalEvent("SIGNAL", 7L, "READY", "alice-id", "bob-id", null)));
        assertThrows(AccessDeniedException.class, () -> controller.signal(42L,
                new RoomWsController.SignalInput(7L, "OFFER", "stranger", "sdp"), () -> "alice"));
        assertThrows(IllegalArgumentException.class, () -> controller.signal(42L,
                new RoomWsController.SignalInput(7L, "OFFER", "bob-id", ""), () -> "alice"));
        assertThrows(IllegalArgumentException.class, () -> controller.signal(42L,
                new RoomWsController.SignalInput(7L, "ICE", "bob-id", "x".repeat(20001)), () -> "alice"));
        verifyNoMoreInteractions(messages);
    }
}
