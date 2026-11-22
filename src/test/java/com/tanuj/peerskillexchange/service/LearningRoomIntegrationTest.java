package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.board.BoardOperationRequest;
import com.tanuj.peerskillexchange.dto.board.Point;
import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.enums.Role;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.ExchangeRequestRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import com.tanuj.peerskillexchange.repository.LearningRoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LearningRoomIntegrationTest {
    @Autowired UserRepository users;
    @Autowired ExchangeRequestRepository exchanges;
    @Autowired LearningRoomService rooms;
    @Autowired BoardService board;
    @Autowired BoardProjectionService projection;
    @Autowired LearningRoomRepository roomRepository;
    @Autowired AiSessionService ai;
    @Autowired EntityManager entityManager;

    private ExchangeRequest exchange;

    @BeforeEach
    void setup() {
        User alice = user("alice1", "alice");
        User bob = user("bob001", "bob");
        user("carol1", "carol");
        exchange = new ExchangeRequest();
        exchange.setSender(alice);
        exchange.setReceiver(bob);
        exchange.setStatus(RequestStatus.ACCEPTED);
        exchange = exchanges.saveAndFlush(exchange);
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

    @Test
    void memberCanJoinAndOnlyOwnerCanCloseOneRoomPerExchange() {
        var room = rooms.create("alice", exchange.getId());
        assertEquals(room.id(), rooms.create("bob", exchange.getId()).id());
        assertEquals(room.id(), rooms.join("bob", room.id()).id());
        assertThrows(AccessDeniedException.class, () -> rooms.join("carol", room.id()));
        assertThrows(AccessDeniedException.class, () -> rooms.close("bob", room.id()));
        assertEquals("CLOSED", rooms.close("alice", room.id()).status());
        assertThrows(ResponseStatusException.class, () -> rooms.join("alice", room.id()));
    }

    @Test
    void operationsKeepOrderAndReloadFromDatabaseAfterClear() {
        var room = rooms.create("alice", exchange.getId());
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        var stroke = new BoardOperationRequest(first, "STROKE",
                List.of(new Point(0.1, 0.2), new Point(0.3, 0.4)), "#ff0000", 3, null);
        assertEquals(1, board.append("alice", room.id(), stroke).sequenceNumber());
        assertEquals(2, board.append("bob", room.id(),
                new BoardOperationRequest(second, "CLEAR", null, null, null, null)).sequenceNumber());
        entityManager.flush();
        entityManager.clear();
        var history = board.history("bob", room.id(), 0);
        assertEquals(List.of(first, second), history.operations().stream().map(o -> o.operationId()).toList());
        assertEquals(2, history.latestSequence());
        assertEquals(List.of(new Point(0.1, 0.2), new Point(0.3, 0.4)), history.operations().get(0).points());
        assertEquals(1, board.history("alice", room.id(), 1).operations().size());
    }

    @Test
    void rejectsDuplicateInvalidAndOutOfRoomOperations() {
        var room = rooms.create("alice", exchange.getId());
        String opId = UUID.randomUUID().toString();
        var stroke = new BoardOperationRequest(opId, "STROKE",
                List.of(new Point(0, 0), new Point(1, 1)), "#112233", 2, null);
        board.append("alice", room.id(), stroke);
        assertThrows(ResponseStatusException.class, () -> board.append("alice", room.id(), stroke));
        assertThrows(ResponseStatusException.class, () -> board.append("alice", room.id(),
                new BoardOperationRequest(UUID.randomUUID().toString(), "STROKE",
                        List.of(new Point(-1, 0), new Point(1, 1)), "#112233", 2, null)));
        assertThrows(AccessDeniedException.class, () -> board.history("carol", room.id(), 0));
        assertThrows(AccessDeniedException.class, () -> board.append("carol", room.id(),
                new BoardOperationRequest(UUID.randomUUID().toString(), "CLEAR", null, null, null, null)));
        exchanges.delete(exchange);
        exchanges.flush();
        assertThrows(AccessDeniedException.class, () -> rooms.join("bob", room.id()));
    }

    @Test
    void doesNotExposeProjectionWhilePublishedEventIsUnapplied() {
        var room = rooms.create("alice", exchange.getId());
        var entity = roomRepository.findById(room.id()).orElseThrow();
        entity.setLastPublishedOffset(4L);
        entity.setLastAppliedOffset(3L);
        roomRepository.saveAndFlush(entity);
        assertThrows(AccessDeniedException.class, () -> projection.get("carol", room.id()));
        var recovering = assertThrows(ResponseStatusException.class,
                () -> projection.get("alice", room.id()));
        assertEquals(503, recovering.getStatusCode().value());
    }

    @Test
    void closedRoomAiJobsAreMemberOnlyIdempotentAndLimited() {
        var room = rooms.create("alice", exchange.getId());
        String requestId = UUID.randomUUID().toString();
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> ai.request("alice", room.id(), requestId)).getStatusCode().value());
        rooms.close("alice", room.id());
        assertEquals("CLOSED", rooms.getForMember("bob", room.id()).status());
        assertEquals(0, board.history("bob", room.id(), 0).latestSequence());
        assertEquals(0, projection.get("bob", room.id()).sequenceNumber());
        assertThrows(AccessDeniedException.class, () -> rooms.getForMember("carol", room.id()));
        assertThrows(AccessDeniedException.class, () -> ai.latest("carol", room.id()));
        assertThrows(AccessDeniedException.class, () -> ai.request("carol", room.id(), requestId));
        var first = ai.request("alice", room.id(), requestId);
        assertEquals("QUEUED", first.status());
        assertEquals(first.id(), ai.request("alice", room.id(), requestId).id());
        assertEquals(first.id(), ai.latest("bob", room.id()).id());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> ai.request("bob", room.id(), UUID.randomUUID().toString())).getStatusCode().value());
        assertTrue(ai.claim(first.id()));
        ai.succeeded(first.id(), new AiSessionGenerator.Result("Practice summary", List.of("One?", "Two?", "Three?")));
        assertFalse(ai.claim(first.id()));
        assertEquals("SUCCEEDED", ai.latest("bob", room.id()).status());
        ai.report("bob", room.id(), first.id(), "UNHELPFUL");
        ai.report("bob", room.id(), first.id(), "UNHELPFUL");
        assertThrows(AccessDeniedException.class, () -> ai.report("carol", room.id(), first.id(), "UNHELPFUL"));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> ai.report("bob", room.id(), first.id(), "BAD")).getStatusCode().value());
        var second = ai.request("bob", room.id(), UUID.randomUUID().toString());
        assertTrue(ai.claim(second.id()));
        assertTrue(ai.failedAttempt(second.id(), "provider_unavailable"));
        assertTrue(ai.claim(second.id()));
        assertTrue(ai.failedAttempt(second.id(), "provider_unavailable"));
        assertTrue(ai.claim(second.id()));
        assertFalse(ai.failedAttempt(second.id(), "provider_unavailable"));
        assertEquals("FAILED", ai.latest("alice", room.id()).status());
        var third = ai.request("alice", room.id(), UUID.randomUUID().toString());
        assertEquals("QUEUED", third.status());
        assertEquals(429, assertThrows(ResponseStatusException.class,
                () -> ai.request("alice", room.id(), UUID.randomUUID().toString())).getStatusCode().value());
    }
}
