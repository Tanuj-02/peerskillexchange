package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.enums.SkillType;
import com.tanuj.peerskillexchange.model.Skill;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.model.UserSkill;
import com.tanuj.peerskillexchange.repository.SkillRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import com.tanuj.peerskillexchange.repository.UserSkillRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserSkillService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    public UserSkillService(UserRepository userRepository, SkillRepository skillRepository, UserSkillRepository userSkillRepository) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
    }

    private void addSkillList(User user, List<String> skillNames, SkillType type) {

        for (String skillName : skillNames) {

            skillName = skillName.trim().toLowerCase();

            Skill skill = skillRepository
                    .findBySkillnameIgnoreCase(skillName)
                    .orElse(null);

            if (skill == null) {
                skill = new Skill();
                skill.setSkillname(skillName);
                skill = skillRepository.save(skill);
            }

            boolean exists = false;

//            for (UserSkill us : user.getUserSkills()) {
//                if (us.getSkill().getId() == skill.getId()
//                        && us.getType() == type) {
//                    exists = true;
//                    break;
//                }
//            }

            for (UserSkill us : user.getUserSkills()) {
                if (us.getSkill().getId() == skill.getId()) {
                    throw new RuntimeException("Skill already added in another category: " + skillName);
                }
            }

            if (!exists) {
                UserSkill userSkill = new UserSkill();
                userSkill.setUsers(user);
                userSkill.setSkill(skill);
                userSkill.setType(type);

                user.getUserSkills().add(userSkill);
            }
        }
    }
    @Transactional
    public String addSkill(String username, List<String> canTeach, List<String> wantsToLearn) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        userSkillRepository.deleteByUsers(user);
        user.getUserSkills().clear();
        if(canTeach != null){
            addSkillList(user,canTeach,SkillType.TEACH);
        }

        if(wantsToLearn != null){
            addSkillList(user,wantsToLearn,SkillType.LEARN);
        }

        userRepository.save(user);

        return "Skills added successfully";
    }
}
