package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.skill.SkillDto;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.model.UserSkill;
import com.tanuj.peerskillexchange.service.UserSkillService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final UserSkillService userSkillService;

    public SkillController(UserSkillService userSkillService) {
        this.userSkillService = userSkillService;
    }

    @PostMapping("/add")
    public String addSkill(@RequestBody SkillDto skillDto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Logic to add skill to the user
        return userSkillService.addSkill(username, skillDto.canTeach(), skillDto.wantsToLearn());
    }
}
