package com.tanuj.peerskillexchange.dto.skill;

import com.tanuj.peerskillexchange.model.Skill;

import java.util.List;

public record SkillDto(
        List<String> canTeach,
        List<String> wantsToLearn// "TEACH" or "LEARN"
) {
}
