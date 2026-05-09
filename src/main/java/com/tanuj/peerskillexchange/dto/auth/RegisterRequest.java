package com.tanuj.peerskillexchange.dto.auth;

import com.tanuj.peerskillexchange.model.Skill;

import java.util.List;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String name,
        String bio,
        String ProfilePicture,
        String college
) {
}
