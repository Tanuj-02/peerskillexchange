package com.tanuj.peerskillexchange.config;

import com.tanuj.peerskillexchange.service.ChatService;
import com.tanuj.peerskillexchange.service.LearningRoomService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatSubscriptionInterceptorTest {
    private static Message<byte[]> frame(StompCommand command, String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(user);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void deniesUnauthenticatedConnect() {
        var interceptor = new ChatSubscriptionInterceptor(mock(ChatService.class), mock(LearningRoomService.class));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null), null));
    }

    @Test
    void checksConversationAccessBeforeSubscribing() {
        ChatService chats = mock(ChatService.class);
        var interceptor = new ChatSubscriptionInterceptor(chats, mock(LearningRoomService.class));
        var message = frame(StompCommand.SUBSCRIBE, "/topic/conversations/42", () -> "alice");
        assertSame(message, interceptor.preSend(message, null));
        verify(chats).assertConversationAccess("alice", 42L);
    }

    @Test
    void rejectsMalformedAndUnapprovedDestinations() {
        var interceptor = new ChatSubscriptionInterceptor(mock(ChatService.class), mock(LearningRoomService.class));
        Principal alice = () -> "alice";
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/conversations/nope", alice), null));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/notifications", alice), null));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, "/topic/conversations/42", alice), null));
    }

    @Test
    void verifiesRoomMembershipForSubscribeAndCursorSend() {
        LearningRoomService rooms = mock(LearningRoomService.class);
        var interceptor = new ChatSubscriptionInterceptor(mock(ChatService.class), rooms);
        Principal alice = () -> "alice";
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/rooms/42", alice), null);
        interceptor.preSend(frame(StompCommand.SEND, "/app/rooms.cursor/42", alice), null);
        interceptor.preSend(frame(StompCommand.SEND, "/app/rooms.signal/42", alice), null);
        verify(rooms, times(3)).assertAccess("alice", 42L);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/rooms/42/other", alice), null));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, "/app/rooms.signal/42/other", alice), null));
    }
}
