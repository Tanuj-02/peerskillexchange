package com.tanuj.peerskillexchange.dto.exchangeRequest;

import com.tanuj.peerskillexchange.dto.user.UserResponse;

import java.time.LocalDateTime;

public record ERResponse(
            Long id,
            UserResponse sender,
            UserResponse receiver,
            String message,
            String status,
            String canTeach,
            String wantToLearn,
            LocalDateTime createdAt
) {
}
