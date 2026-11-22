package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.AiSessionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface AiSessionJobRepository extends JpaRepository<AiSessionJob, Long> {
    Optional<AiSessionJob> findByRoomIdAndRequestId(Long roomId, String requestId);
    Optional<AiSessionJob> findFirstByRoomIdOrderByIdDesc(Long roomId);
    Optional<AiSessionJob> findFirstByCallIdOrderByIdDesc(Long callId);
    long countByCallId(Long callId);
    long countByRoomId(Long roomId);
    List<AiSessionJob> findTop20ByStatusAndPublishedAtIsNullOrderByIdAsc(AiSessionJob.Status status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from AiSessionJob j where j.id = :id")
    Optional<AiSessionJob> lockById(@Param("id") Long id);
}
