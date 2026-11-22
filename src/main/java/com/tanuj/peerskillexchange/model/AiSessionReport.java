package com.tanuj.peerskillexchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_ai_report_user", columnNames = {"job_id", "reporter_id"}))
public class AiSessionReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "job_id", nullable = false)
    private AiSessionJob job;
    @Column(name = "reporter_id", nullable = false)
    private String reporterId;
    @Column(nullable = false)
    private String reason;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
