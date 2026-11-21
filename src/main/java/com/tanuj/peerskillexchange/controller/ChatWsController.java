package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.chat.ChatMessageResponse;
import com.tanuj.peerskillexchange.dto.chat.SendMessageRequest;
import com.tanuj.peerskillexchange.service.ChatService;
import com.tanuj.peerskillexchange.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
public class ChatWsController {

    private final ChatService chatService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWsController(
            ChatService chatService,
            NotificationService notificationService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatService = chatService;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(SendMessageRequest request, Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        ChatMessageResponse saved = chatService.sendMessage(principal.getName(), request.receiverId(), request.content());

        notificationService.notifyChatMessage(saved);

        messagingTemplate.convertAndSendToUser(saved.senderUsername(), "/queue/messages", saved);
        messagingTemplate.convertAndSendToUser(saved.receiverUsername(), "/queue/messages", saved);

        messagingTemplate.convertAndSend("/topic/conversations/" + saved.conversationId(), saved);
    }

    @MessageExceptionHandler
    public void handleException(Throwable ex, Principal principal) {
        if (principal == null || principal.getName() == null) return;
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Message could not be processed.");
    }
}

