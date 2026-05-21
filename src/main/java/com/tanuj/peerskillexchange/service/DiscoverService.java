package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.user.UserResponse;
import com.tanuj.peerskillexchange.enums.SkillType;
import com.tanuj.peerskillexchange.mapper.UserMapper;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DiscoverService {

    private final UserRepository userRepository;
    private final UserSkillPageLoader userSkillPageLoader;

    public DiscoverService(UserRepository userRepository, UserSkillPageLoader userSkillPageLoader) {
        this.userRepository = userRepository;
        this.userSkillPageLoader = userSkillPageLoader;
    }

    public Page<UserResponse> discoverForUser(String username, String q, Pageable pageable) {
        userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        Page<String> idPage = userRepository.findDiscoverOrderedIds(
                username,
                query,
                SkillType.TEACH,
                SkillType.LEARN,
                pageable
        );

        return userSkillPageLoader.loadPage(idPage, pageable).map(UserMapper::toUserResponse);
    }
}

