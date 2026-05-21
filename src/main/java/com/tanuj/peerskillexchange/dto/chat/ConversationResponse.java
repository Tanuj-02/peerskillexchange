package com.tanuj.peerskillexchange.dto.chat;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        String otherUserId,
        String otherUsername,
        String otherName,
        String otherProfilePicture,
        LocalDateTime createdAt
) {
}

