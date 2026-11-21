package com.tanuj.peerskillexchange.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tanuj.peerskillexchange.dto.skill.SkillDto;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
