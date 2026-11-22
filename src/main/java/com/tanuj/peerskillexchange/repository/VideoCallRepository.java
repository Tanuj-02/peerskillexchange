package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.VideoCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface VideoCallRepository extends JpaRepository<VideoCall, Long> {
    List<VideoCall> findByRoomIdOrderByStartedAtDesc(Long roomId);
    Optional<VideoCall> findFirstByRoomIdAndEndedAtIsNullOrderByStartedAtDesc(Long roomId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LearningRoom r where r.id = :roomId")
    Optional<com.tanuj.peerskillexchange.model.LearningRoom> lockRoom(Long roomId);
}
