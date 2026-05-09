package com.tanuj.peerskillexchange.dto.user;

public record UserRequest(
        String id,
        String name,
        String username,
        String email,
        String profilePicture,
        String bio,
        String college
) {
}
