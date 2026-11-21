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
        return toUserResponse(user, true);
    }

    public static UserResponse toPublicUserResponse(User user) {
        return toUserResponse(user, false);
    }

    private static UserResponse toUserResponse(User user, boolean owner) {

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
                owner ? user.getEmail() : null,
                owner ? user.getRole().name() : null,
                skillDto
        );
    }

    public static void toUser(UserRequest userRequest, User user){
        if (userRequest.name() != null) {
            if (userRequest.name().isBlank()) {
                throw new IllegalArgumentException("Name cannot be blank.");
            }
            user.setName(userRequest.name().trim());
        }
        user.setProfilePicture(userRequest.profilePicture());
        user.setBio(userRequest.bio());
        user.setCollege(userRequest.college());
    }
}
