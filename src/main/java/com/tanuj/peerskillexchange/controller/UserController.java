package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.user.UserRequest;
import com.tanuj.peerskillexchange.dto.user.PasswordChangeRequest;
import com.tanuj.peerskillexchange.dto.user.EmailChangeRequest;
import com.tanuj.peerskillexchange.dto.user.EmailChangeVerificationRequest;
import com.tanuj.peerskillexchange.dto.user.UserResponse;
import com.tanuj.peerskillexchange.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByUsername(username);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable String id) {
        return userService.getById(id);
    }

    @PutMapping("/me/update")
    public UserResponse updateUser(@RequestBody UserRequest userRequest) {
        try {
            return userService.updateUser(userRequest);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/me/update")
    public UserResponse updateUserPost(@RequestBody UserRequest userRequest) {
        try {
            return userService.updateUser(userRequest);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/me/password")
    public String updatePassword(@Valid @RequestBody PasswordChangeRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            userService.updatePassword(username, request.oldPassword(), request.newPassword(), request.verificationCode());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        return "Password updated successfully";
    }

    @PostMapping("/me/password/send-verification")
    public String sendPasswordVerification() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            userService.sendPasswordChangeVerificationCode(username);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        return "Verification code sent to your email";
    }

    @PostMapping("/me/email-change")
    public String requestEmailChange(@Valid @RequestBody EmailChangeRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            userService.requestEmailChange(username, request.email());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        return "Verification code sent to the new email address";
    }

    @PostMapping("/me/email-change/verify")
    public String verifyEmailChange(@Valid @RequestBody EmailChangeVerificationRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            userService.verifyEmailChange(username, request.code());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        return "Email address updated";
    }

    @GetMapping("/search")
    public Page<UserResponse> searchUsers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "BOTH") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        try {
            return userService.searchUsers(q, skill, type, pageable);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
