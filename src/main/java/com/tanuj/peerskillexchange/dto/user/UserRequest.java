package com.tanuj.peerskillexchange.dto.user;

import com.tanuj.peerskillexchange.dto.skill.SkillDto;

public record UserRequest(
        String id,
        String name,
        String username,
        String email,
        String profilePicture,
        String bio,
        String college,
        SkillDto skills
) {
}
