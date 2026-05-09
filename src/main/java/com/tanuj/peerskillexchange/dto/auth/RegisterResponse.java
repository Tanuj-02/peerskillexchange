package com.tanuj.peerskillexchange.dto.auth;

public record RegisterResponse(
            String id,
            String username,
            String email,
            String name,
            String bio,
            String ProfilePicture,
            String college,
            String role
) {
}
