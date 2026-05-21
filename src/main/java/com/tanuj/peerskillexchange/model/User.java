package com.tanuj.peerskillexchange.model;

import com.tanuj.peerskillexchange.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(length = 6, unique = true, nullable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean emailVerified = false;

    private String emailVerificationCode;

    private LocalDateTime emailVerificationExpiry;

    private String passwordChangeCode;

    private LocalDateTime passwordChangeCodeExpiry;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_bin")
    private String username;

    @Column(nullable = false)
    private String password;

    private String profilePicture;

    private String college;

    @Column(length = 2500)
    private String bio;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSkill> userSkills = new ArrayList<>();

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExchangeRequest> sentRequests;

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExchangeRequest> receivedRequests;
}

