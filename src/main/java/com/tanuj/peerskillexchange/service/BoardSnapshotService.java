package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanuj.peerskillexchange.dto.board.BoardOperationResponse;
import com.tanuj.peerskillexchange.model.BoardSnapshot;
import com.tanuj.peerskillexchange.model.LearningRoom;
import com.tanuj.peerskillexchange.repository.BoardOperationRepository;
import com.tanuj.peerskillexchange.repository.BoardSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BoardSnapshotService {
    private static final Logger log = LoggerFactory.getLogger(BoardSnapshotService.class);
    private final BoardSnapshotRepository snapshots;
    private final BoardOperationRepository operations;
    private final BoardProjectionAssembler assembler;
    private final ObjectMapper json;
    private final int interval;

    public BoardSnapshotService(BoardSnapshotRepository snapshots, BoardOperationRepository operations,
                                BoardProjectionAssembler assembler, ObjectMapper json,
                                @Value("${board.snapshot.interval:20}") int interval) {
        this.snapshots = snapshots;
        this.operations = operations;
        this.assembler = assembler;
        this.json = json;
        this.interval = Math.max(1, interval);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveIfDue(LearningRoom room) {
        if (room.getSequenceNumber() == 0 || room.getSequenceNumber() % interval != 0) return;
        BoardSnapshot snapshot = snapshots.findByRoomId(room.getId()).orElseGet(BoardSnapshot::new);
        List<BoardOperationResponse> strokes = rebuild(room);
        snapshot.setRoom(room);
        snapshot.setSequenceNumber(room.getSequenceNumber());
        snapshot.setKafkaPartition(room.getKafkaPartition());
        snapshot.setKafkaOffset(BoardService.offset(room.getLastAppliedOffset()));
        try {
            snapshot.setStrokesJson(json.writeValueAsString(strokes));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not create board snapshot.", e);
        }
        snapshot.setUpdatedAt(LocalDateTime.now());
        snapshots.saveAndFlush(snapshot);
    }

    @Transactional(readOnly = true)
    public List<BoardOperationResponse> rebuild(LearningRoom room) {
        BoardSnapshot snapshot = snapshots.findByRoomId(room.getId()).orElse(null);
        List<BoardOperationResponse> seed = List.of();
        long after = 0;
        if (snapshot != null && snapshot.getSequenceNumber() <= room.getSequenceNumber()
                && snapshot.getKafkaOffset() <= BoardService.offset(room.getLastAppliedOffset())) {
            try {
                seed = json.readValue(snapshot.getStrokesJson(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.warn("Board snapshot invalid; replaying SQL history room={}", room.getId());
                seed = List.of();
                snapshot = null;
            }
            if (snapshot != null) after = snapshot.getSequenceNumber();
        }
        List<BoardOperationResponse> tail = new ArrayList<>();
        while (after < room.getSequenceNumber()) {
            var page = operations.findByRoomIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                    room.getId(), after, PageRequest.of(0, 500));
            if (page.isEmpty()) throw new IllegalStateException("Board history has a gap.");
            for (var op : page) {
                if (op.getSequenceNumber() > room.getSequenceNumber()) break;
                if (op.getSequenceNumber() != after + 1) {
                    throw new IllegalStateException("Board history has a gap.");
                }
                tail.add(assembler.toResponse(op));
                after = op.getSequenceNumber();
            }
        }
        return assembler.visible(seed, tail);
    }
}
