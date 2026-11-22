package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.board.BoardOperationRequest;
import com.tanuj.peerskillexchange.dto.board.Point;
import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.enums.Role;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.BoardOperationRepository;
import com.tanuj.peerskillexchange.repository.BoardPublicationRepository;
import com.tanuj.peerskillexchange.repository.BoardSnapshotRepository;
import com.tanuj.peerskillexchange.repository.ExchangeRequestRepository;
import com.tanuj.peerskillexchange.repository.LearningRoomRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"board.kafka.enabled=true", "board.snapshot.interval=2",
        "logging.level.org.apache.kafka=ERROR", "logging.level.kafka=ERROR"})
@EmbeddedKafka(partitions = 3, topics = "pse-board-operations-v1",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class BoardKafkaIntegrationTest {
    @Autowired UserRepository users;
    @Autowired ExchangeRequestRepository exchanges;
    @Autowired LearningRoomService rooms;
    @Autowired LearningRoomRepository roomRepository;
    @Autowired BoardPipelineService pipeline;
    @Autowired BoardProjectionService projection;
    @Autowired BoardSnapshotRepository snapshots;
    @Autowired BoardOperationRepository operations;
    @Autowired BoardPublicationRepository publications;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired KafkaListenerEndpointRegistry listeners;
    @Autowired BoardKafkaPublisher publisher;
    @Autowired BoardService board;

    @Test
    void publishesInOrderDeduplicatesAndRebuildsAfterRedisOutage() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User alice = user("alice_" + suffix);
        User bob = user("bob_" + suffix);
        ExchangeRequest exchange = new ExchangeRequest();
        exchange.setSender(alice);
        exchange.setReceiver(bob);
        exchange.setStatus(RequestStatus.ACCEPTED);
        exchange = exchanges.saveAndFlush(exchange);
        var room = rooms.create(alice.getUsername(), exchange.getId());
        var first = new BoardOperationRequest(UUID.randomUUID().toString(), "STROKE",
                List.of(new Point(0, 0), new Point(1, 1)), "#112233", 3, null);
        var second = new BoardOperationRequest(UUID.randomUUID().toString(), "STROKE",
                List.of(new Point(0, 1), new Point(1, 0)), "#445566", 4, null);
        assertEquals(1, pipeline.append(alice.getUsername(), room.id(), first).sequenceNumber());
        assertEquals(1, pipeline.append(alice.getUsername(), room.id(), first).sequenceNumber());
        assertEquals(2, pipeline.append(bob.getUsername(), room.id(), second).sequenceNumber());
        var original = publications.findByRoomIdAndOperationId(room.id(), first.operationId()).orElseThrow();
        long duplicateOffset = kafka.send("pse-board-operations-v1", room.id().toString(),
                original.getEventJson()).join().getRecordMetadata().offset();
        long deadline = System.currentTimeMillis() + 5000;
        while (roomRepository.findById(room.id()).orElseThrow().getLastAppliedOffset() < duplicateOffset
                && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(25); } catch (InterruptedException e) { Thread.currentThread().interrupt(); fail(e); }
        }
        assertEquals(2, operations.count());
        assertEquals(2, snapshots.findByRoomId(room.id()).orElseThrow().getSequenceNumber());
        var rebuilt = projection.get(alice.getUsername(), room.id());
        assertEquals(2, rebuilt.sequenceNumber());
        assertEquals(2, rebuilt.strokes().size());
        assertEquals("database", rebuilt.source());
        var damagedSnapshot = snapshots.findByRoomId(room.id()).orElseThrow();
        damagedSnapshot.setStrokesJson("invalid-json");
        snapshots.saveAndFlush(damagedSnapshot);
        assertEquals(2, projection.get(alice.getUsername(), room.id()).strokes().size());
        var savedRoom = roomRepository.findById(room.id()).orElseThrow();
        assertEquals(savedRoom.getLastPublishedOffset(), savedRoom.getLastAppliedOffset());

        // A publication is durable while the projection worker is stopped; reads must not claim it is current.
        listeners.getListenerContainers().forEach(MessageListenerContainer::stop);
        var third = new BoardOperationRequest(UUID.randomUUID().toString(), "CLEAR",
                null, null, null, null);
        publisher.publish(alice.getUsername(), room.id(), third);
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> board.history(alice.getUsername(), room.id(), 0)).getStatusCode().value());
        listeners.getListenerContainers().forEach(MessageListenerContainer::start);
        deadline = System.currentTimeMillis() + 5000;
        while (operations.count() < 3 && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(25); } catch (InterruptedException e) { Thread.currentThread().interrupt(); fail(e); }
        }
        assertEquals(3, operations.count());
        assertTrue(projection.get(bob.getUsername(), room.id()).strokes().isEmpty());
    }

    private User user(String username) {
        User user = new User();
        user.setId(username.substring(0, 1) + username.substring(username.length() - 5));
        user.setUsername(username);
        user.setName(username);
        user.setEmail(username + "@example.test");
        user.setPassword("test-only-hash");
        user.setRole(Role.USER);
        return users.saveAndFlush(user);
    }
}
