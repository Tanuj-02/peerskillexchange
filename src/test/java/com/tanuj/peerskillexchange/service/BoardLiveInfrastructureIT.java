package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.board.BoardOperationRequest;
import com.tanuj.peerskillexchange.dto.board.Point;
import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.enums.Role;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.ExchangeRequestRepository;
import com.tanuj.peerskillexchange.repository.LearningRoomRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Explicitly selected with -Dtest=BoardLiveInfrastructureIT after starting compose.board.yml.
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"board.kafka.enabled=true",
        "spring.kafka.listener.auto-startup=true", "board.kafka.topic=pse-board-live-check-v1",
        "spring.kafka.consumer.group-id=pse-board-live-check-v1", "board.snapshot.interval=2",
        "logging.level.org.apache.kafka=ERROR", "logging.level.kafka=ERROR"})
class BoardLiveInfrastructureIT {
    @Autowired UserRepository users;
    @Autowired ExchangeRequestRepository exchanges;
    @Autowired LearningRoomService rooms;
    @Autowired BoardPipelineService pipeline;
    @Autowired BoardProjectionService projection;
    @Autowired RoomPresenceService presence;
    @Autowired LearningRoomRepository roomRepository;
    @Autowired StringRedisTemplate redis;

    @Test
    void liveKafkaAndRedisSupportProjectionCacheFlushAndPresence() {
        String suffix = UUID.randomUUID().toString().substring(0, 5);
        User alice = user("a" + suffix, "alice_" + suffix);
        User bob = user("b" + suffix, "bob_" + suffix);
        ExchangeRequest exchange = new ExchangeRequest();
        exchange.setSender(alice);
        exchange.setReceiver(bob);
        exchange.setStatus(RequestStatus.ACCEPTED);
        exchange = exchanges.saveAndFlush(exchange);
        var room = rooms.create(alice.getUsername(), exchange.getId());
        String projectionKey = "pse:board:" + room.id() + ":projection";
        redis.delete(projectionKey);

        var stroke = new BoardOperationRequest(UUID.randomUUID().toString(), "STROKE",
                List.of(new Point(0.1, 0.2), new Point(0.8, 0.9)), "#123456", 3, null);
        assertEquals(1, pipeline.append(alice.getUsername(), room.id(), stroke).sequenceNumber());
        assertEquals("database", projection.get(bob.getUsername(), room.id()).source());
        assertEquals("redis", projection.get(bob.getUsername(), room.id()).source());
        assertEquals(Boolean.TRUE, redis.hasKey(projectionKey));

        pipeline.append(bob.getUsername(), room.id(), new BoardOperationRequest(
                UUID.randomUUID().toString(), "CLEAR", null, null, null, null));
        assertTrue(projection.get(alice.getUsername(), room.id()).strokes().isEmpty());
        redis.delete(projectionKey);
        assertEquals("database", projection.get(alice.getUsername(), room.id()).source());
        assertEquals("redis", projection.get(alice.getUsername(), room.id()).source());

        presence.touch(room.id(), alice.getId());
        String presenceKey = "pse:board:" + room.id() + ":presence:" + alice.getId();
        assertEquals("1", redis.opsForValue().get(presenceKey));
        assertTrue(redis.getExpire(presenceKey) > 0);
        var savedRoom = roomRepository.findById(room.id()).orElseThrow();
        assertEquals(savedRoom.getLastPublishedOffset(), savedRoom.getLastAppliedOffset());
    }

    private User user(String id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setName(username);
        user.setEmail(username + "@example.test");
        user.setPassword("test-only-hash");
        user.setRole(Role.USER);
        return users.saveAndFlush(user);
    }
}
