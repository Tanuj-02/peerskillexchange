package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.BoardPublication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardPublicationRepository extends JpaRepository<BoardPublication, Long> {
    Optional<BoardPublication> findByRoomIdAndOperationId(Long roomId, String operationId);
}
