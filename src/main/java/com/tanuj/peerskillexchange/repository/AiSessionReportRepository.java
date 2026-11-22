package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.AiSessionReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSessionReportRepository extends JpaRepository<AiSessionReport, Long> {
    boolean existsByJobIdAndReporterId(Long jobId, String reporterId);
}
