package com.tanuj.peerskillexchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_call_note_author_request", columnNames = {"call_id", "author_id", "request_id"}))
public class VideoCallNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "call_id", nullable = false)
    private VideoCall call;
    @Column(name = "author_id", nullable = false)
    private String authorId;
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(nullable = false, length = 1000)
    private String content;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
