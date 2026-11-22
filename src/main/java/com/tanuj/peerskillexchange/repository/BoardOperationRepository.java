package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.BoardOperation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardOperationRepository extends JpaRepository<BoardOperation, Long> {
    boolean existsByRoomIdAndOperationId(Long roomId, String operationId);
    Optional<BoardOperation> findByRoomIdAndOperationId(Long roomId, String operationId);
    boolean existsByRoomIdAndOperationIdAndType(Long roomId, String operationId, BoardOperation.BoardOperationType type);
    boolean existsByRoomIdAndTargetOperationIdAndType(Long roomId, String targetOperationId,
                                                       BoardOperation.BoardOperationType type);
    List<BoardOperation> findByRoomIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            Long roomId, long sequenceNumber, Pageable pageable);
}
