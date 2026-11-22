package com.tanuj.peerskillexchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class BoardSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false) @JoinColumn(name = "room_id", nullable = false, unique = true)
    private LearningRoom room;

    @Column(nullable = false)
    private long sequenceNumber;

    private Integer kafkaPartition;

    @Column(nullable = false)
    private long kafkaOffset;

    @Lob @Column(nullable = false)
    private String strokesJson;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
