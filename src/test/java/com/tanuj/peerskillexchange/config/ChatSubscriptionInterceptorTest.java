package com.tanuj.peerskillexchange.config;

import com.tanuj.peerskillexchange.service.ChatService;
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
        var interceptor = new ChatSubscriptionInterceptor(mock(ChatService.class));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null), null));
    }

    @Test
    void checksConversationAccessBeforeSubscribing() {
        ChatService chats = mock(ChatService.class);
        var interceptor = new ChatSubscriptionInterceptor(chats);
        var message = frame(StompCommand.SUBSCRIBE, "/topic/conversations/42", () -> "alice");
        assertSame(message, interceptor.preSend(message, null));
        verify(chats).assertConversationAccess("alice", 42L);
    }

    @Test
    void rejectsMalformedAndUnapprovedDestinations() {
        var interceptor = new ChatSubscriptionInterceptor(mock(ChatService.class));
        Principal alice = () -> "alice";
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/conversations/nope", alice), null));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/notifications", alice), null));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                frame(StompCommand.SEND, "/topic/conversations/42", alice), null));
    }
}
