package com.tanuj.peerskillexchange.dto.notification;

import com.tanuj.peerskillexchange.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        boolean read,
        String actorUserId,
        String actorUsername,
        Long conversationId,
        Long exchangeRequestId,
        Long chatMessageId,
        LocalDateTime createdAt
) {
}
