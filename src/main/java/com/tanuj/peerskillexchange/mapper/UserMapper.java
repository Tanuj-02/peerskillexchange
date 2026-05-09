package com.tanuj.peerskillexchange.mapper;

import com.tanuj.peerskillexchange.dto.skill.SkillDto;
import com.tanuj.peerskillexchange.dto.user.UserRequest;
import com.tanuj.peerskillexchange.dto.user.UserResponse;
import com.tanuj.peerskillexchange.enums.SkillType;
import com.tanuj.peerskillexchange.model.Skill;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.model.UserSkill;

import java.util.ArrayList;
import java.util.List;

public class UserMapper {
    public static UserResponse toUserResponse(User user){

        List<String> canTeach = new ArrayList<>();
        List<String> wantsToLearn = new ArrayList<>();

        List<UserSkill> userSkills = user.getUserSkills();

        if(userSkills != null){
            for(UserSkill us : userSkills){

                String skillName = us.getSkill().getSkillname();

                if(us.getType() == SkillType.TEACH){
                    canTeach.add(skillName);
                }
                else if(us.getType() == SkillType.LEARN){
                    wantsToLearn.add(skillName);
                }
            }
        }

        SkillDto skillDto = new SkillDto(canTeach, wantsToLearn);

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getBio(),
                user.getCollege(),
                user.getProfilePicture(),
                user.getEmail(),
                user.getRole().name(),
                skillDto
        );
    }

    public static void toUser(UserRequest userRequest, User user){
        user.setName(userRequest.name());
        user.setUsername(userRequest.username());
        user.setEmail(userRequest.email());
        user.setProfilePicture(userRequest.profilePicture());
        user.setBio(userRequest.bio());
        user.setCollege(userRequest.college());
    }
}
