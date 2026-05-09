package com.tanuj.peerskillexchange.dto.auth;

public record Login(
        String usernameOrEmail,
        String password
) {
}
