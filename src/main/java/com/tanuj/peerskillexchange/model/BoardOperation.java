package com.tanuj.peerskillexchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_board_operation_id", columnNames = {"room_id", "operation_id"}),
        @UniqueConstraint(name = "uk_board_operation_sequence", columnNames = {"room_id", "sequence_number"})
})
public class BoardOperation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "room_id", nullable = false)
    private LearningRoom room;

    @Column(name = "operation_id", nullable = false, length = 36)
    private String operationId;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    private Integer kafkaPartition;

    private Long kafkaOffset;

    @ManyToOne(optional = false) @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private BoardOperationType type;

    @Column(length = 16000)
    private String pointsJson;

    @Column(length = 9)
    private String color;

    private Integer width;

    @Column(length = 36)
    private String targetOperationId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum BoardOperationType { STROKE, ERASE, CLEAR }
}
