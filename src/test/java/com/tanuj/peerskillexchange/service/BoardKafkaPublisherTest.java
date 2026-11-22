package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanuj.peerskillexchange.dto.board.BoardOperationRequest;
import com.tanuj.peerskillexchange.model.BoardOperation;
import com.tanuj.peerskillexchange.model.LearningRoom;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.BoardPublicationRepository;
import com.tanuj.peerskillexchange.repository.LearningRoomRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BoardKafkaPublisherTest {
    @Test
    void brokerFailureDoesNotCreatePublicationOrAcknowledgeOperation() {
        LearningRoomService access = mock(LearningRoomService.class);
        LearningRoomRepository rooms = mock(LearningRoomRepository.class);
        BoardPublicationRepository publications = mock(BoardPublicationRepository.class);
        BoardService board = mock(BoardService.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        User author = new User();
        author.setId("alice1");
        LearningRoom room = new LearningRoom();
        room.setId(12L);
        var input = new BoardOperationRequest(UUID.randomUUID().toString(), "CLEAR",
                null, null, null, null);
        when(access.currentUser("alice")).thenReturn(author);
        when(rooms.lockById(12L)).thenReturn(Optional.of(room));
        when(board.validateInput(input)).thenReturn(BoardOperation.BoardOperationType.CLEAR);
        when(publications.findByRoomIdAndOperationId(12L, input.operationId())).thenReturn(Optional.empty());
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        var publisher = new BoardKafkaPublisher(access, rooms, publications, board, kafka,
                new ObjectMapper(), new SimpleMeterRegistry(), "board-test");
        var failure = assertThrows(ResponseStatusException.class,
                () -> publisher.publish("alice", 12L, input));
        assertEquals(503, failure.getStatusCode().value());
        verify(publications, never()).saveAndFlush(any());
        verify(rooms, never()).save(any());
    }
}
