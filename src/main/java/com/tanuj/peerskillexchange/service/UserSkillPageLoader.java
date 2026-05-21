package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class UserSkillPageLoader {

    private final UserRepository userRepository;

    public UserSkillPageLoader(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<User> loadPage(Page<String> idPage, Pageable pageable) {
        List<String> ids = idPage.getContent();
        long total = idPage.getTotalElements();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        List<User> loaded = userRepository.findAllWithSkillsByIdIn(ids);
        Map<String, User> byId = loaded.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<User> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, total);
    }
}
