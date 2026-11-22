package com.tanuj.peerskillexchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(indexes = @Index(name = "idx_video_call_room_started", columnList = "room_id,started_at"))
public class VideoCall {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "room_id", nullable = false)
    private LearningRoom room;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime endedAt;

    @Column(nullable = false)
    private String startedBy;
}
