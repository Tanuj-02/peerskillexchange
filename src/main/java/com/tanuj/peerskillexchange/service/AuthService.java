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

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {
    private static final int REGISTER_VERIFICATION_EXPIRY_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final jwtService jwtService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, jwtService jwtService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
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

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email is not verified");
        }

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
        user.setEmailVerified(false);
        user.setEmailVerificationCode(generateSixDigitCode());
        user.setEmailVerificationExpiry(LocalDateTime.now().plusMinutes(REGISTER_VERIFICATION_EXPIRY_MINUTES));

        User savedUser = userRepository.save(user);
        emailService.sendVerificationCode(
                savedUser.getEmail(),
                "Verify your PeerSkillExchange account",
                savedUser.getEmailVerificationCode(),
                REGISTER_VERIFICATION_EXPIRY_MINUTES
        );

        return RegisterMapper.toRegisterResponse(savedUser);
    }

    public String verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            return "Email already verified";
        }

        if (user.getEmailVerificationCode() == null || !user.getEmailVerificationCode().equals(code)) {
            throw new RuntimeException("Invalid verification code");
        }

        if (user.getEmailVerificationExpiry() == null || user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiry(null);
        userRepository.save(user);

        return "Email verified successfully";
    }

    public String resendEmailVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            return "Email already verified";
        }

        user.setEmailVerificationCode(generateSixDigitCode());
        user.setEmailVerificationExpiry(LocalDateTime.now().plusMinutes(REGISTER_VERIFICATION_EXPIRY_MINUTES));
        userRepository.save(user);

        emailService.sendVerificationCode(
                user.getEmail(),
                "Your new PeerSkillExchange verification code",
                user.getEmailVerificationCode(),
                REGISTER_VERIFICATION_EXPIRY_MINUTES
        );

        return "Verification code sent";
    }

    private String generateSixDigitCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
