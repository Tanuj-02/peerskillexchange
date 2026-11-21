package com.tanuj.peerskillexchange.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailChangeRequest(@NotBlank @Email String email) {}
