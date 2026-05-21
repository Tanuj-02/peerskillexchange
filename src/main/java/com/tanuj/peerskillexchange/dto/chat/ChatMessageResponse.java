package com.tanuj.peerskillexchange.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long conversationId,
        String senderId,
        String senderUsername,
        String receiverId,
        String receiverUsername,
        String content,
        LocalDateTime createdAt
) {
}

