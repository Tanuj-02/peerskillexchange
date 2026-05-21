package com.tanuj.peerskillexchange.dto.skill;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record SkillDto(
        @JsonAlias({"can_teach"})
        List<String> canTeach,
        @JsonAlias({"wants_to_learn"})
        List<String> wantsToLearn
) {
}
