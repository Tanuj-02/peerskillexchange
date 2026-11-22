package com.tanuj.peerskillexchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_ai_job_request", columnNames = {"room_id", "request_id"}),
       indexes = @Index(name = "idx_ai_job_call_id", columnList = "call_id,id"))
public class AiSessionJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "room_id", nullable = false)
    private LearningRoom room;
    @ManyToOne @JoinColumn(name = "call_id")
    private VideoCall call;
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(nullable = false)
    private String requestedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.QUEUED;
    @Column(nullable = false)
    private int attempts;
    private LocalDateTime publishedAt;
    @Column(length = 4000)
    private String summary;
    @Column(length = 4000)
    private String questionsJson;
    @Column(length = 64)
    private String errorCode;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;
    public enum Status { QUEUED, RUNNING, SUCCEEDED, FAILED }
}
