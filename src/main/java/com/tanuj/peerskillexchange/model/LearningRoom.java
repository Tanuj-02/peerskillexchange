package com.tanuj.peerskillexchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_room_exchange", columnNames = "exchange_request_id"))
public class LearningRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_request_id", nullable = false)
    private Long exchangeRequestId;

    @ManyToOne(optional = false) @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(optional = false) @JoinColumn(name = "peer_id", nullable = false)
    private User peer;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RoomStatus status = RoomStatus.OPEN;

    @Column(nullable = false)
    private long sequenceNumber;

    private Long lastPublishedOffset = -1L;

    private Long lastAppliedOffset = -1L;

    private Integer kafkaPartition;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    public enum RoomStatus { OPEN, CLOSED }
}
