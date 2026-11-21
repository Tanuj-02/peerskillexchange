package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.user.UserRequest;
import com.tanuj.peerskillexchange.dto.user.UserResponse;
import com.tanuj.peerskillexchange.enums.SkillType;
import com.tanuj.peerskillexchange.mapper.UserMapper;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.security.SecureRandom;

@Service
public class UserService {
    private static final int PASSWORD_VERIFICATION_EXPIRY_MINUTES = 10;
    private static final SecureRandom CODE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EmailService emailService;
    private final UserSkillService userSkillService;
    private final UserSkillPageLoader userSkillPageLoader;

    public UserService(
            UserRepository userRepository,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            EmailService emailService,
            UserSkillService userSkillService,
            UserSkillPageLoader userSkillPageLoader
    ) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.emailService = emailService;
        this.userSkillService = userSkillService;
        this.userSkillPageLoader = userSkillPageLoader;
    }


    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        return UserMapper.toUserResponse(user);
    }

    public UserResponse getById(String id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        return UserMapper.toPublicUserResponse(user);
    }


    @Transactional
    public UserResponse updateUser(UserRequest userRequest) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        if (userRequest.username() != null && !user.getUsername().equals(userRequest.username())) {
            throw new IllegalArgumentException("Username changes are not supported.");
        }
        if (userRequest.email() != null && !user.getEmail().equalsIgnoreCase(userRequest.email())) {
            throw new IllegalArgumentException("Email changes require verification and are not supported here.");
        }
        UserMapper.toUser(userRequest, user);

        userRepository.save(user);

        String username = user.getUsername();
        if (userRequest.skills() != null) {
            userSkillService.addSkill(username, userRequest.skills().canTeach(), userRequest.skills().wantsToLearn());
        }

        User updatedUser = userRepository.findById(user.getId()).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        return UserMapper.toUserResponse(updatedUser);
    }

    @Transactional
    public void sendPasswordChangeVerificationCode(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        if (user.getPasswordChangeCodeSentAt() != null
                && user.getPasswordChangeCodeSentAt().plusMinutes(1).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Please wait before requesting another code.");
        }
        String verificationCode = generateSixDigitCode();
        user.setPasswordChangeCode(bCryptPasswordEncoder.encode(verificationCode));
        user.setPasswordChangeCodeExpiry(LocalDateTime.now().plusMinutes(PASSWORD_VERIFICATION_EXPIRY_MINUTES));
        user.setPasswordChangeCodeSentAt(LocalDateTime.now());
        user.setPasswordChangeAttempts(0);
        userRepository.save(user);

        emailService.sendVerificationCode(
                user.getEmail(),
                "PeerSkillExchange password change verification",
                verificationCode,
                PASSWORD_VERIFICATION_EXPIRY_MINUTES
        );
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void updatePassword(String username, String oldPassword, String newPassword, String verificationCode) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        if (!bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password");
        }

        if (bCryptPasswordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        if (user.getPasswordChangeCode() == null || user.getPasswordChangeCodeExpiry() == null
                || !user.getPasswordChangeCodeExpiry().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code expired");
        }
        if (user.getPasswordChangeAttempts() >= 5) {
            throw new IllegalArgumentException("Too many attempts. Request a new code.");
        }
        if (verificationCode == null || !bCryptPasswordEncoder.matches(verificationCode, user.getPasswordChangeCode())) {
            user.setPasswordChangeAttempts(user.getPasswordChangeAttempts() + 1);
            userRepository.save(user);
            throw new IllegalArgumentException("Invalid verification code");
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        user.setPasswordChangeCode(null);
        user.setPasswordChangeCodeExpiry(null);
        user.setPasswordChangeCodeSentAt(null);
        user.setPasswordChangeAttempts(0);
        userRepository.save(user);
    }

    @Transactional
    public void requestEmailChange(String username, String newEmail) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        String normalizedEmail = newEmail == null ? "" : newEmail.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank() || normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Enter a different email address.");
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email address is already in use.");
        }
        if (user.getPendingEmailSentAt() != null
                && user.getPendingEmailSentAt().plusMinutes(1).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Please wait before requesting another code.");
        }
        String code = generateSixDigitCode();
        user.setPendingEmail(normalizedEmail);
        user.setPendingEmailCodeHash(bCryptPasswordEncoder.encode(code));
        user.setPendingEmailExpiry(LocalDateTime.now().plusMinutes(10));
        user.setPendingEmailSentAt(LocalDateTime.now());
        user.setPendingEmailAttempts(0);
        userRepository.save(user);
        emailService.sendVerificationCode(normalizedEmail, "Verify your new email address", code, 10);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyEmailChange(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (user.getPendingEmail() == null || user.getPendingEmailCodeHash() == null
                || user.getPendingEmailExpiry() == null
                || !user.getPendingEmailExpiry().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Email change code is missing or expired.");
        }
        if (user.getPendingEmailAttempts() >= 5) {
            throw new IllegalArgumentException("Too many attempts. Request a new code.");
        }
        if (code == null || !bCryptPasswordEncoder.matches(code, user.getPendingEmailCodeHash())) {
            user.setPendingEmailAttempts(user.getPendingEmailAttempts() + 1);
            userRepository.save(user);
            throw new IllegalArgumentException("Invalid email change code.");
        }
        if (userRepository.findByEmail(user.getPendingEmail()).isPresent()) {
            throw new IllegalArgumentException("Email address is already in use.");
        }
        user.setEmail(user.getPendingEmail());
        user.setEmailVerified(true);
        user.setPendingEmail(null);
        user.setPendingEmailCodeHash(null);
        user.setPendingEmailExpiry(null);
        user.setPendingEmailSentAt(null);
        user.setPendingEmailAttempts(0);
        userRepository.save(user);
    }

    private static String normalizeSkillName(String skillName) {
        if (skillName == null) return null;
        String normalized = skillName.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static SkillType parseSkillType(String type) {
        if (type == null) return null;
        String t = type.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case "TEACH" -> SkillType.TEACH;
            case "LEARN" -> SkillType.LEARN;
            case "BOTH", "" -> null;
            default -> throw new IllegalArgumentException("Invalid type. Use TEACH, LEARN, or BOTH.");
        };
    }

    public Page<UserResponse> searchUsers(String q, String skill, String type, Pageable pageable) {
        String query = (q == null ? "" : q.trim().toLowerCase(Locale.ROOT));
        SkillType skillType = parseSkillType(type);

        Page<String> idPage;
        if (skill == null || normalizeSkillName(skill) == null) {
            idPage = userRepository.searchUserIds(query, pageable);
        } else {
            String normalizedSkill = normalizeSkillName(skill);
            idPage = userRepository.searchUserIdsBySkill(query, normalizedSkill, skillType, pageable);
        }
        return userSkillPageLoader.loadPage(idPage, pageable).map(UserMapper::toPublicUserResponse);
    }

    private String generateSixDigitCode() {
        return String.valueOf(CODE_RANDOM.nextInt(900000) + 100000);
    }
}
