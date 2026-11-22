package com.tanuj.peerskillexchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_board_publication", columnNames = {"room_id", "operation_id"}))
public class BoardPublication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "room_id", nullable = false)
    private LearningRoom room;

    @Column(name = "operation_id", nullable = false, length = 36)
    private String operationId;

    @Lob @Column(nullable = false, columnDefinition = "TEXT")
    private String eventJson;

    @Column(nullable = false)
    private int kafkaPartition;

    @Column(nullable = false)
    private long kafkaOffset;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
