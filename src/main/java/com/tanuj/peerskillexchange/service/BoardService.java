package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanuj.peerskillexchange.dto.board.*;
import com.tanuj.peerskillexchange.model.BoardOperation;
import com.tanuj.peerskillexchange.model.LearningRoom;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.BoardOperationRepository;
import com.tanuj.peerskillexchange.repository.LearningRoomRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
public class BoardService {
    private static final int HISTORY_PAGE_SIZE = 500;
    private final LearningRoomService roomService;
    private final LearningRoomRepository rooms;
    private final BoardOperationRepository operations;
    private final ObjectMapper objectMapper;
    private final UserRepository users;
    private final BoardSnapshotService snapshots;
    private final BoardProjectionAssembler assembler;

    public BoardService(LearningRoomService roomService, LearningRoomRepository rooms,
                        BoardOperationRepository operations, ObjectMapper objectMapper, UserRepository users,
                        BoardSnapshotService snapshots, BoardProjectionAssembler assembler) {
        this.roomService = roomService;
        this.rooms = rooms;
        this.operations = operations;
        this.objectMapper = objectMapper;
        this.users = users;
        this.snapshots = snapshots;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public BoardHistoryResponse history(String username, Long roomId, long after) {
        if (after < 0) throw new ResponseStatusException(BAD_REQUEST, "Sequence cannot be negative.");
        LearningRoom room = roomService.requireMemberRoom(username, roomId);
        assertCurrent(room);
        List<BoardOperationResponse> page = operations
                .findByRoomIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                        roomId, after, PageRequest.of(0, HISTORY_PAGE_SIZE))
                .stream().map(this::toResponse).toList();
        long last = page.isEmpty() ? after : page.get(page.size() - 1).sequenceNumber();
        return new BoardHistoryResponse(page, room.getSequenceNumber(), last < room.getSequenceNumber());
    }

    @Transactional
    public BoardOperationResponse append(String username, Long roomId, BoardOperationRequest input) {
        BoardOperation.BoardOperationType type = validateInput(input);
        String operationId = input.operationId();

        User me = roomService.currentUser(username);
        LearningRoom room = rooms.lockById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Room not found."));
        roomService.assertOpenMember(me, room);
        if (operations.existsByRoomIdAndOperationId(roomId, operationId)) {
            throw new ResponseStatusException(CONFLICT, "Operation already exists.");
        }
        validateEraseTarget(roomId, input, type);

        return saveOperation(room, me, input, type, null, null);
    }

    public BoardOperation.BoardOperationType validateInput(BoardOperationRequest input) {
        if (input == null) throw new ResponseStatusException(BAD_REQUEST, "Operation is required.");
        requireUuid(input.operationId(), "operationId");
        BoardOperation.BoardOperationType type;
        try {
            type = BoardOperation.BoardOperationType.valueOf(input.type());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported board operation.");
        }
        validatePayload(type, input);
        return type;
    }

    public void validateEraseTarget(Long roomId, BoardOperationRequest input,
                                    BoardOperation.BoardOperationType type) {
        if (type != BoardOperation.BoardOperationType.ERASE) return;
        if (!operations.existsByRoomIdAndOperationIdAndType(roomId, input.targetOperationId(),
                BoardOperation.BoardOperationType.STROKE)) {
            throw new ResponseStatusException(BAD_REQUEST, "Stroke does not exist in this room.");
        }
        if (operations.existsByRoomIdAndTargetOperationIdAndType(roomId, input.targetOperationId(),
                BoardOperation.BoardOperationType.ERASE)) {
            throw new ResponseStatusException(CONFLICT, "Stroke is already erased.");
        }
    }

    @Transactional
    public BoardOperationResponse applyPublishedEvent(BoardKafkaEvent event, int partition, long offset) {
        if (event.schemaVersion() != 1 || event.roomId() == null || event.authorId() == null) {
            throw new IllegalArgumentException("Unsupported board event.");
        }
        BoardOperationRequest input = event.asRequest();
        BoardOperation.BoardOperationType type = validateInput(input);
        LearningRoom room = rooms.lockById(event.roomId())
                .orElseThrow(() -> new IllegalStateException("Published room no longer exists."));
        if (room.getKafkaPartition() != null && room.getKafkaPartition() != partition) {
            throw new IllegalStateException("Kafka partition changed for this room.");
        }
        room.setKafkaPartition(partition);
        room.setLastPublishedOffset(Math.max(offset(room.getLastPublishedOffset()), offset));
        var existing = operations.findByRoomIdAndOperationId(event.roomId(), event.operationId());
        if (existing.isPresent()) {
            room.setLastAppliedOffset(Math.max(offset(room.getLastAppliedOffset()), offset));
            rooms.save(room);
            return null;
        }
        User author = users.findById(event.authorId())
                .orElseThrow(() -> new IllegalStateException("Published author no longer exists."));
        if (!author.getId().equals(room.getOwner().getId()) && !author.getId().equals(room.getPeer().getId())) {
            throw new IllegalStateException("Published author is not a room member.");
        }
        validateEraseTarget(event.roomId(), input, type);
        room.setLastAppliedOffset(offset);
        return saveOperation(room, author, input, type, partition, offset);
    }

    private BoardOperationResponse saveOperation(LearningRoom room, User me, BoardOperationRequest input,
                                                 BoardOperation.BoardOperationType type,
                                                 Integer partition, Long offset) {
        room.setSequenceNumber(room.getSequenceNumber() + 1);
        BoardOperation operation = new BoardOperation();
        operation.setRoom(room);
        operation.setOperationId(input.operationId());
        operation.setSequenceNumber(room.getSequenceNumber());
        operation.setKafkaPartition(partition);
        operation.setKafkaOffset(offset);
        operation.setAuthor(me);
        operation.setType(type);
        if (type == BoardOperation.BoardOperationType.STROKE) {
            try {
                operation.setPointsJson(objectMapper.writeValueAsString(input.points()));
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid stroke points.");
            }
            operation.setColor(input.color());
            operation.setWidth(input.width());
        }
        if (type == BoardOperation.BoardOperationType.ERASE) {
            operation.setTargetOperationId(input.targetOperationId());
        }
        rooms.save(room);
        BoardOperation saved = operations.saveAndFlush(operation);
        snapshots.saveIfDue(room);
        return toResponse(saved);
    }

    public static long offset(Long value) { return value == null ? -1L : value; }

    public void assertCurrent(LearningRoom room) {
        if (offset(room.getLastPublishedOffset()) > offset(room.getLastAppliedOffset())) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Board is recovering. Retry shortly.");
        }
    }

    private void validatePayload(BoardOperation.BoardOperationType type, BoardOperationRequest input) {
        if (type == BoardOperation.BoardOperationType.STROKE) {
            if (input.points() == null || input.points().size() < 2 || input.points().size() > 256
                    || input.points().stream().anyMatch(p -> p == null || !Double.isFinite(p.x())
                    || !Double.isFinite(p.y()) || p.x() < 0 || p.x() > 1 || p.y() < 0 || p.y() > 1)
                    || input.color() == null || !input.color().matches("#[0-9a-fA-F]{6}")
                    || input.width() == null || input.width() < 1 || input.width() > 20
                    || input.targetOperationId() != null) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid stroke.");
            }
        } else if (type == BoardOperation.BoardOperationType.ERASE) {
            requireUuid(input.targetOperationId(), "targetOperationId");
            if (input.points() != null || input.color() != null || input.width() != null) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid erase operation.");
            }
        } else if (input.points() != null || input.color() != null || input.width() != null
                || input.targetOperationId() != null) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid clear operation.");
        }
    }

    private String requireUuid(String value, String field) {
        try {
            if (value == null || value.length() != 36) throw new IllegalArgumentException();
            UUID.fromString(value);
            return value;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, field + " must be a UUID.");
        }
    }

    public BoardOperationResponse toResponse(BoardOperation op) {
        return assembler.toResponse(op);
    }
}
