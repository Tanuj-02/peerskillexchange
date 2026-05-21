package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.chat.ChatMessageResponse;
import com.tanuj.peerskillexchange.dto.chat.ConversationResponse;
import com.tanuj.peerskillexchange.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public Page<ConversationResponse> myConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending());
        return chatService.listMyConversations(username, pageable);
    }

    @GetMapping("/{conversationId}/messages")
    public Page<ChatMessageResponse> messages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending());
        try {
            return chatService.getMessages(username, conversationId, pageable);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }

    @PostMapping("/{conversationId}/messages")
    public ChatMessageResponse sendMessage(
            @PathVariable Long conversationId,
            @RequestBody MessageBodyRequest request
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return chatService.sendMessageToConversation(username, conversationId, request.content());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    public record MessageBodyRequest(String content) {}
}

