package com.tanuj.peerskillexchange.dto.exchangeRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ERRequest(
        @NotBlank String receiverId,
        String message,
        @NotEmpty List<@NotBlank String> canTeach,
        @NotEmpty List<@NotBlank String> wantToLearn
) {
}
