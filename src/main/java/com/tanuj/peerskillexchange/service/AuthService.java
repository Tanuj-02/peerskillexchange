package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.auth.Login;
import com.tanuj.peerskillexchange.dto.auth.LoginResponse;
import com.tanuj.peerskillexchange.dto.auth.RegisterRequest;
import com.tanuj.peerskillexchange.dto.auth.RegisterResponse;
import com.tanuj.peerskillexchange.enums.Role;
import com.tanuj.peerskillexchange.mapper.RegisterMapper;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.UserRepository;
import com.tanuj.peerskillexchange.security.jwt.jwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final jwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, jwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String generateUniqueId() {
        String id;

        do {
            int number = ThreadLocalRandom.current().nextInt(100000, 1000000);
            id = String.valueOf(number);
        } while (userRepository.existsById(id));

        return id;
    }

    public LoginResponse login(Login login) {
        User user = userRepository.findByEmail(login.usernameOrEmail())
                .or(()-> userRepository.findByUsername(login.usernameOrEmail()))
                .orElseThrow(() -> new RuntimeException("User not found with email or username: " + login.usernameOrEmail()));

        if (!passwordEncoder.matches(login.password(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
    }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(user.getUsername(), user.getEmail(), token);
    }


    public RegisterResponse register(RegisterRequest registerRequest) {
        if(userRepository.findByUsername(registerRequest.username()).isPresent()){
            throw new RuntimeException("Username already exists");
        }
        if(userRepository.findByEmail(registerRequest.email()).isPresent()){
            throw new RuntimeException("Email already exists");
        }

        User user = RegisterMapper.toUserEntity(registerRequest);
        user.setId(generateUniqueId());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        return RegisterMapper.toRegisterResponse(savedUser);
    }

}
