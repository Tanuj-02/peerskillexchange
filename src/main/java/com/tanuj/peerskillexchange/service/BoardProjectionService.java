package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanuj.peerskillexchange.dto.board.BoardProjectionResponse;
import com.tanuj.peerskillexchange.model.LearningRoom;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class BoardProjectionService {
    private static final Logger log = LoggerFactory.getLogger(BoardProjectionService.class);
    private static final Duration TTL = Duration.ofMinutes(30);
    private final LearningRoomService rooms;
    private final BoardService board;
    private final BoardSnapshotService snapshots;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final MeterRegistry metrics;

    public BoardProjectionService(LearningRoomService rooms, BoardService board,
                                  BoardSnapshotService snapshots, StringRedisTemplate redis,
                                  ObjectMapper json, MeterRegistry metrics) {
        this.rooms = rooms;
        this.board = board;
        this.snapshots = snapshots;
        this.redis = redis;
        this.json = json;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public BoardProjectionResponse get(String username, Long roomId) {
        LearningRoom room = rooms.requireMemberRoom(username, roomId);
        board.assertCurrent(room);
        String key = "pse:board:" + roomId + ":projection";
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                BoardProjectionResponse value = json.readValue(cached, BoardProjectionResponse.class);
                if (value.sequenceNumber() == room.getSequenceNumber()
                        && value.kafkaOffset() == BoardService.offset(room.getLastAppliedOffset())) {
                    metrics.counter("board.redis.hit").increment();
                    return new BoardProjectionResponse(value.sequenceNumber(), value.kafkaOffset(), value.strokes(), "redis");
                }
            }
        } catch (Exception e) {
            redisFailure(roomId, e);
        }
        BoardProjectionResponse rebuilt = new BoardProjectionResponse(room.getSequenceNumber(),
                BoardService.offset(room.getLastAppliedOffset()), snapshots.rebuild(room), "database");
        try {
            redis.opsForValue().set(key, json.writeValueAsString(rebuilt), TTL);
        } catch (Exception e) {
            redisFailure(roomId, e);
        }
        metrics.counter("board.redis.rebuild").increment();
        return rebuilt;
    }

    private void redisFailure(Long roomId, Exception failure) {
        metrics.counter("board.redis.failures").increment();
        log.warn("Board Redis projection unavailable room={} cause={}", roomId,
                failure.getClass().getSimpleName());
    }
}
