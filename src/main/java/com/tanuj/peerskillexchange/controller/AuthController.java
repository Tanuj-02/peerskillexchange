package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.auth.Login;
import com.tanuj.peerskillexchange.dto.auth.LoginResponse;
import com.tanuj.peerskillexchange.dto.auth.RegisterRequest;
import com.tanuj.peerskillexchange.dto.auth.RegisterResponse;
import com.tanuj.peerskillexchange.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/login")
    public LoginResponse login(@RequestBody Login login){
        return authService.login(login);
    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest registerRequest){
        return authService.register(registerRequest);
    }
}
