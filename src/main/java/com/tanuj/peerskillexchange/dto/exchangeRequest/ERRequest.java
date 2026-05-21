package com.tanuj.peerskillexchange.dto.exchangeRequest;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ERRequest(
        @NotBlank String receiverId,
        String message,
        List<String> canTeach,
        List<String> wantToLearn
) {
}
