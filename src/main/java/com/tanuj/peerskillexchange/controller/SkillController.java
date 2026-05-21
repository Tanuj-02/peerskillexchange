package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.skill.SkillDto;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.model.UserSkill;
import com.tanuj.peerskillexchange.service.UserSkillService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
        try {
            return userSkillService.addSkill(username, skillDto.canTeach(), skillDto.wantsToLearn());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
