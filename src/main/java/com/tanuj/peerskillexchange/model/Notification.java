package com.tanuj.peerskillexchange.model;

import com.tanuj.peerskillexchange.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean viewed = false;

    @Column(length = 64)
    private String actorUserId;

    @Column(length = 255)
    private String actorUsername;

    private Long conversationId;
    private Long exchangeRequestId;
    private Long chatMessageId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
