package com.tanuj.peerskillexchange.mapper;

import com.tanuj.peerskillexchange.dto.skill.SkillDto;
import com.tanuj.peerskillexchange.enums.SkillType;
import com.tanuj.peerskillexchange.model.Skill;
import com.tanuj.peerskillexchange.model.UserSkill;

import java.util.ArrayList;
import java.util.List;

public class SkillMapper {
    public static SkillDto toSkillDto(List<UserSkill> userSkills){
        List<String> canTeach = new ArrayList<>();
        List<String> wantsToLearn = new ArrayList<>();

        for (UserSkill us : userSkills) {

            String skillName = us.getSkill().getSkillname();

            if (us.getType() == SkillType.TEACH) {
                canTeach.add(skillName);
            } else if (us.getType() == SkillType.LEARN) {
                wantsToLearn.add(skillName);
            }
        }

        return new SkillDto(canTeach, wantsToLearn);
    }
}
