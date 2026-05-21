package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.user.UserResponse;
import com.tanuj.peerskillexchange.service.DiscoverService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private final DiscoverService discoverService;

    public DiscoverController(DiscoverService discoverService) {
        this.discoverService = discoverService;
    }

    @GetMapping
    public Page<UserResponse> discover(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return discoverService.discoverForUser(username, q, pageable);
    }
}

