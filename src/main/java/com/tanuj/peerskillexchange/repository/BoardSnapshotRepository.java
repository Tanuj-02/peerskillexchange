package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.BoardSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardSnapshotRepository extends JpaRepository<BoardSnapshot, Long> {
    Optional<BoardSnapshot> findByRoomId(Long roomId);
}
