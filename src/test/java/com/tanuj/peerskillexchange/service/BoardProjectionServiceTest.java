package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanuj.peerskillexchange.dto.board.BoardProjectionResponse;
import com.tanuj.peerskillexchange.model.LearningRoom;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BoardProjectionServiceTest {
    @Test
    void currentRedisProjectionAvoidsSqlReplay() throws Exception {
        var fixture = fixture();
        when(fixture.values.get(anyString())).thenReturn(fixture.json.writeValueAsString(
                new BoardProjectionResponse(2, 1, List.of(), "database")));
        var result = fixture.service.get("alice", 7L);
        assertEquals("redis", result.source());
        verify(fixture.snapshots, never()).rebuild(any());
    }

    @Test
    void staleRedisProjectionRebuildsFromDatabase() throws Exception {
        var fixture = fixture();
        when(fixture.values.get(anyString())).thenReturn(fixture.json.writeValueAsString(
                new BoardProjectionResponse(1, 0, List.of(), "redis")));
        var result = fixture.service.get("alice", 7L);
        assertEquals(2, result.sequenceNumber());
        assertEquals("database", result.source());
        verify(fixture.snapshots).rebuild(fixture.room);
        verify(fixture.values).set(anyString(), anyString(), any());
    }

    @Test
    void redisOutageStillReturnsAuthorizedDatabaseProjection() {
        var fixture = fixture();
        when(fixture.values.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        var result = fixture.service.get("alice", 7L);
        assertEquals("database", result.source());
        verify(fixture.rooms).requireMemberRoom("alice", 7L);
        verify(fixture.snapshots).rebuild(fixture.room);
    }

    private Fixture fixture() {
        LearningRoomService rooms = mock(LearningRoomService.class);
        BoardService board = mock(BoardService.class);
        BoardSnapshotService snapshots = mock(BoardSnapshotService.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        LearningRoom room = new LearningRoom();
        room.setId(7L);
        room.setSequenceNumber(2);
        room.setLastAppliedOffset(1L);
        when(rooms.requireMemberRoom("alice", 7L)).thenReturn(room);
        when(snapshots.rebuild(room)).thenReturn(List.of());
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        return new Fixture(new BoardProjectionService(rooms, board, snapshots, redis, json,
                new SimpleMeterRegistry()), rooms, snapshots, values, room, json);
    }

    private record Fixture(BoardProjectionService service, LearningRoomService rooms,
                           BoardSnapshotService snapshots, ValueOperations<String, String> values,
                           LearningRoom room, ObjectMapper json) {}
}
