package com.tanuj.peerskillexchange.dto.exchangeRequest;

import com.tanuj.peerskillexchange.dto.user.UserResponse;

import java.time.LocalDateTime;
import java.util.List;

public record ERResponse(
            Long id,
            UserResponse sender,
            UserResponse receiver,
            String message,
            String status,
            List<String> canTeach,
            List<String> wantToLearn,
            LocalDateTime createdAt
) {
}
