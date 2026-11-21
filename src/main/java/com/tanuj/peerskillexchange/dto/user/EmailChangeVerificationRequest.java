package com.tanuj.peerskillexchange.dto.user;

import jakarta.validation.constraints.NotBlank;

public record EmailChangeVerificationRequest(@NotBlank String code) {}
