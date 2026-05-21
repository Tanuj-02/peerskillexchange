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

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserService {
    private static final int PASSWORD_VERIFICATION_EXPIRY_MINUTES = 10;

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

        return UserMapper.toUserResponse(user);
    }


    public UserResponse updateUser(UserRequest userRequest) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow(
                () -> new RuntimeException("User not found")
        );

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

    public void sendPasswordChangeVerificationCode(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        String verificationCode = generateSixDigitCode();
        user.setPasswordChangeCode(verificationCode);
        user.setPasswordChangeCodeExpiry(LocalDateTime.now().plusMinutes(PASSWORD_VERIFICATION_EXPIRY_MINUTES));
        userRepository.save(user);

        emailService.sendVerificationCode(
                user.getEmail(),
                "PeerSkillExchange password change verification",
                verificationCode,
                PASSWORD_VERIFICATION_EXPIRY_MINUTES
        );
    }

    public void updatePassword(String username, String oldPassword, String newPassword, String verificationCode) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new RuntimeException("User not found")
        );

        if (!bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Incorrect current password");
        }

        if (bCryptPasswordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }

        if (user.getPasswordChangeCode() == null || !user.getPasswordChangeCode().equals(verificationCode)) {
            throw new RuntimeException("Invalid verification code");
        }

        if (user.getPasswordChangeCodeExpiry() == null || user.getPasswordChangeCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code expired");
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        user.setPasswordChangeCode(null);
        user.setPasswordChangeCodeExpiry(null);
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
        return userSkillPageLoader.loadPage(idPage, pageable).map(UserMapper::toUserResponse);
    }

    private String generateSixDigitCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
