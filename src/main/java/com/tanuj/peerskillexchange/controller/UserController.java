package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.user.UserRequest;
import com.tanuj.peerskillexchange.dto.user.UserResponse;
import com.tanuj.peerskillexchange.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    public UserResponse updateUser( @RequestBody UserRequest userRequest) {
        return userService.updateUser(userRequest);
    }

    @PutMapping("/me/password")
    public String updatePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updatePassword(username, oldPassword, newPassword);
        return "Password updated successfully";
    }
}
