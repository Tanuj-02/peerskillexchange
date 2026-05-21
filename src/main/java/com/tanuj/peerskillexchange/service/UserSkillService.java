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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private static String normalizeSkillName(String skillName) {
        if (skillName == null) return null;
        String normalized = skillName.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private void addSkillList(User user, Set<String> skillNames, SkillType type) {

        for (String skillName : skillNames) {

            Skill skill = skillRepository
                    .findBySkillnameIgnoreCase(skillName)
                    .orElse(null);

            if (skill == null) {
                skill = new Skill();
                skill.setSkillname(skillName);
                skill = skillRepository.save(skill);
            }

            for (UserSkill us : user.getUserSkills()) {
                if (us.getSkill().getId() == skill.getId()) {
                    if (us.getType() != type) {
                        throw new IllegalArgumentException("Skill cannot be in both canTeach and wantsToLearn: " + skillName);
                    }
                }
            }

            UserSkill userSkill = new UserSkill();
            userSkill.setUsers(user);
            userSkill.setSkill(skill);
            userSkill.setType(type);

            user.getUserSkills().add(userSkill);
        }
    }
    @Transactional
    public String addSkill(String username, List<String> canTeach, List<String> wantsToLearn) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        Set<String> canTeachSet = new HashSet<>();
        if (canTeach != null) {
            for (String s : canTeach) {
                String n = normalizeSkillName(s);
                if (n != null) canTeachSet.add(n);
            }
        }

        Set<String> wantsToLearnSet = new HashSet<>();
        if (wantsToLearn != null) {
            for (String s : wantsToLearn) {
                String n = normalizeSkillName(s);
                if (n != null) wantsToLearnSet.add(n);
            }
        }

        if (!canTeachSet.isEmpty() && !wantsToLearnSet.isEmpty()) {
            Set<String> overlap = new HashSet<>(canTeachSet);
            overlap.retainAll(wantsToLearnSet);
            if (!overlap.isEmpty()) {
                throw new IllegalArgumentException("Same skill cannot be in both canTeach and wantsToLearn: " + overlap);
            }
        }

        userSkillRepository.deleteByUsers(user);
        user.getUserSkills().clear();
        addSkillList(user, canTeachSet, SkillType.TEACH);

        addSkillList(user, wantsToLearnSet, SkillType.LEARN);

        userRepository.save(user);

        return "Skills added successfully";
    }
}
