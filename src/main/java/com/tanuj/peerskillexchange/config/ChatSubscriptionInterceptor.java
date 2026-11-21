package com.tanuj.peerskillexchange.config;

import com.tanuj.peerskillexchange.service.ChatService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class ChatSubscriptionInterceptor implements ChannelInterceptor {
    private final ChatService chatService;

    public ChatSubscriptionInterceptor(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command != StompCommand.CONNECT && command != StompCommand.SUBSCRIBE
                && command != StompCommand.SEND) return message;
        Principal principal = accessor.getUser();
        if (principal == null) throw new AccessDeniedException("Authentication required.");
        if (command == StompCommand.CONNECT) return message;
        String destination = accessor.getDestination();
        if (command == StompCommand.SEND) {
            if (!"/app/chat.send".equals(destination)) throw new AccessDeniedException("Destination denied.");
        } else if (destination != null && destination.startsWith("/topic/conversations/")) {
            String id = destination.substring("/topic/conversations/".length());
            try {
                chatService.assertConversationAccess(principal.getName(), Long.parseLong(id));
            } catch (NumberFormatException e) {
                throw new AccessDeniedException("Destination denied.");
            }
        } else if (!"/user/queue/messages".equals(destination)
                && !"/user/queue/errors".equals(destination)) {
            throw new AccessDeniedException("Destination denied.");
        }
        return message;
    }
}
