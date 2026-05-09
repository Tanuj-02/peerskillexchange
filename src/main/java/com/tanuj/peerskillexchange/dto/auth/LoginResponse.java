package com.tanuj.peerskillexchange.dto.auth;

public record LoginResponse(
        String username,
        String email,
        String token
) {
}
