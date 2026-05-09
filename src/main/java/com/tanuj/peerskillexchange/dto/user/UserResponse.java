package com.tanuj.peerskillexchange.dto.user;

import com.tanuj.peerskillexchange.dto.skill.SkillDto;

import java.util.List;

public record UserResponse(
        String id,
        String name,
        String username,
        String bio,
        String college,
        String profilePicture,
        String email,
        String role,
        SkillDto skills
) {
}
