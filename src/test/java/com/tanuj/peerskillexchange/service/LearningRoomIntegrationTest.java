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
    @Autowired BoardSnapshotService snapshots;
    @Autowired LearningRoomRepository roomRepository;
    @Autowired AiSessionService ai;
    @Autowired VideoCallService calls;
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
        var call = calls.start("alice", room.id());
        assertEquals(room.id(), rooms.create("bob", exchange.getId()).id());
        assertEquals(room.id(), rooms.join("bob", room.id()).id());
        assertThrows(AccessDeniedException.class, () -> rooms.join("carol", room.id()));
        assertThrows(AccessDeniedException.class, () -> rooms.close("bob", room.id()));
        assertEquals("CLOSED", rooms.close("alice", room.id()).status());
        assertNotNull(calls.list("bob", room.id()).stream().filter(c -> c.id().equals(call.id())).findFirst().orElseThrow().endedAt());
        assertThrows(ResponseStatusException.class, () -> rooms.join("alice", room.id()));
    }

    @Test
    void ownerCanRestartClosedRoomWithoutLosingBoardOrCallHistory() {
        var room = rooms.create("alice", exchange.getId());
        String operationId = UUID.randomUUID().toString();
        board.append("alice", room.id(), new BoardOperationRequest(operationId, "STROKE",
                List.of(new Point(0.1, 0.2), new Point(0.3, 0.4)), "#112233", 3, null));
        var firstCall = calls.start("bob", room.id());
        rooms.close("alice", room.id());
        assertThrows(AccessDeniedException.class, () -> rooms.reopen("bob", room.id()));
        assertThrows(AccessDeniedException.class, () -> rooms.reopen("carol", room.id()));
        var restarted = rooms.reopen("alice", room.id());
        assertEquals("OPEN", restarted.status());
        assertNull(restarted.closedAt());
        assertEquals(room.id(), restarted.id());
        assertEquals(1, restarted.latestSequence());
        assertEquals(operationId, board.history("bob", room.id(), 0).operations().get(0).operationId());
        assertEquals(1, calls.list("alice", room.id()).size());
        assertNotNull(calls.list("alice", room.id()).get(0).endedAt());
        assertNotEquals(firstCall.id(), calls.start("alice", room.id()).id());
        assertEquals(room.id(), rooms.reopen("alice", room.id()).id());
        rooms.close("alice", room.id());
        exchange.setStatus(RequestStatus.REJECTED);
        exchanges.saveAndFlush(exchange);
        assertThrows(AccessDeniedException.class, () -> rooms.reopen("alice", room.id()));
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
    void shapesAndTextSurviveReloadAndCanBeErased() {
        var room = rooms.create("alice", exchange.getId());
        String shapeId = UUID.randomUUID().toString();
        String textId = UUID.randomUUID().toString();
        board.append("alice", room.id(), new BoardOperationRequest(shapeId, "SHAPE",
                List.of(new Point(0.1, 0.1), new Point(0.5, 0.5)), "#112233", 3,
                null, "RECTANGLE", null));
        board.append("bob", room.id(), new BoardOperationRequest(textId, "TEXT",
                List.of(new Point(0.2, 0.2)), "#112233", 3,
                null, null, "Architecture"));
        board.append("alice", room.id(), new BoardOperationRequest(UUID.randomUUID().toString(),
                "ERASE", null, null, null, shapeId));
        entityManager.flush(); entityManager.clear();
        var items = snapshots.rebuild(roomRepository.findById(room.id()).orElseThrow());
        assertEquals(1, items.size());
        assertEquals("Architecture", items.get(0).text());
        assertThrows(ResponseStatusException.class, () -> board.append("alice", room.id(),
                new BoardOperationRequest(UUID.randomUUID().toString(), "SHAPE",
                        List.of(new Point(0, 0), new Point(1, 1)), "#112233", 3,
                        null, "SCRIPT", null)));
    }

    @Test
    void videoCallsAreMemberOnlyAndAiJobsAreScopedToEndedCallsWithNotes() {
        var room = rooms.create("alice", exchange.getId());
        var call = calls.start("alice", room.id());
        assertEquals(call.id(), calls.start("bob", room.id()).id());
        assertThrows(AccessDeniedException.class, () -> calls.list("carol", room.id()));
        assertThrows(ResponseStatusException.class, () -> ai.requestForCall("alice", room.id(), call.id(), UUID.randomUUID().toString()));
        calls.addNote("bob", room.id(), call.id(), UUID.randomUUID().toString(), "We discussed REST API design.");
        assertEquals(1, calls.notes("alice", room.id(), call.id()).size());
        assertThrows(AccessDeniedException.class, () -> calls.addNote("carol", room.id(), call.id(),
                UUID.randomUUID().toString(), "Not a member"));
        calls.end("bob", room.id(), call.id());
        assertThrows(ResponseStatusException.class, () -> calls.addNote("alice", room.id(), call.id(),
                UUID.randomUUID().toString(), "Late note"));
        String requestId = UUID.randomUUID().toString();
        var job = ai.requestForCall("alice", room.id(), call.id(), requestId);
        assertEquals(job.id(), ai.requestForCall("alice", room.id(), call.id(), requestId).id());
        assertTrue(ai.context(job.id()).contains("REST API design"));
        assertFalse(ai.context(job.id()).contains("Session chat excerpts"));
        assertEquals(job.id(), ai.latestForCall("bob", room.id(), call.id()).id());
        assertNotEquals(call.id(), calls.start("alice", room.id()).id());
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
