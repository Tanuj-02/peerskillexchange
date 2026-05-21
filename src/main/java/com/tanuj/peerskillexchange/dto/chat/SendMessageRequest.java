package com.tanuj.peerskillexchange.dto.chat;

public record SendMessageRequest(
        String receiverId,
        String content
) {
}

