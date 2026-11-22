package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanuj.peerskillexchange.dto.board.BoardKafkaEvent;
import com.tanuj.peerskillexchange.dto.board.BoardOperationRequest;
import com.tanuj.peerskillexchange.model.BoardPublication;
import com.tanuj.peerskillexchange.model.LearningRoom;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.BoardPublicationRepository;
import com.tanuj.peerskillexchange.repository.LearningRoomRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.*;

@Service
public class BoardKafkaPublisher {
    private static final Logger log = LoggerFactory.getLogger(BoardKafkaPublisher.class);
    private final LearningRoomService roomService;
    private final LearningRoomRepository rooms;
    private final BoardPublicationRepository publications;
    private final BoardService board;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final MeterRegistry metrics;
    private final String topic;

    public BoardKafkaPublisher(LearningRoomService roomService, LearningRoomRepository rooms,
                               BoardPublicationRepository publications, BoardService board,
                               KafkaTemplate<String, String> kafka, ObjectMapper json,
                               MeterRegistry metrics, @Value("${board.kafka.topic}") String topic) {
        this.roomService = roomService;
        this.rooms = rooms;
        this.publications = publications;
        this.board = board;
        this.kafka = kafka;
        this.json = json;
        this.metrics = metrics;
        this.topic = topic;
    }

    public record Receipt(String operationId, int partition, long offset) {}

    @Transactional
    public Receipt publish(String username, Long roomId, BoardOperationRequest input) {
        var type = board.validateInput(input);
        User author = roomService.currentUser(username);
        LearningRoom room = rooms.lockById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Room not found."));
        roomService.assertOpenMember(author, room);

        BoardKafkaEvent event = BoardKafkaEvent.from(roomId, author.getId(), input);
        String eventJson;
        try {
            eventJson = json.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid board operation.");
        }
        var existing = publications.findByRoomIdAndOperationId(roomId, input.operationId());
        if (existing.isPresent()) {
            BoardPublication prior = existing.get();
            if (!prior.getEventJson().equals(eventJson)) {
                throw new ResponseStatusException(CONFLICT, "Operation ID has different content.");
            }
            return new Receipt(input.operationId(), prior.getKafkaPartition(), prior.getKafkaOffset());
        }
        board.assertCurrent(room);
        board.validateEraseTarget(roomId, input, type);

        long started = System.nanoTime();
        try {
            var metadata = kafka.send(topic, roomId.toString(), eventJson)
                    .get(7, TimeUnit.SECONDS).getRecordMetadata();
            if (room.getKafkaPartition() != null && room.getKafkaPartition() != metadata.partition()) {
                throw new IllegalStateException("Kafka partition changed for this room.");
            }
            room.setKafkaPartition(metadata.partition());
            room.setLastPublishedOffset(metadata.offset());
            rooms.save(room);
            BoardPublication publication = new BoardPublication();
            publication.setRoom(room);
            publication.setOperationId(input.operationId());
            publication.setEventJson(eventJson);
            publication.setKafkaPartition(metadata.partition());
            publication.setKafkaOffset(metadata.offset());
            publications.saveAndFlush(publication);
            Timer.builder("board.kafka.publish.duration").register(metrics)
                    .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
            return new Receipt(input.operationId(), metadata.partition(), metadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            publishFailure(roomId, input.operationId(), e);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Board log unavailable. Retry with the same operation ID.");
        } catch (Exception e) {
            publishFailure(roomId, input.operationId(), e);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Board log unavailable. Retry with the same operation ID.");
        }
    }

    private void publishFailure(Long roomId, String operationId, Exception failure) {
        metrics.counter("board.kafka.publish.failures").increment();
        log.warn("Board publication failed room={} operation={} cause={}",
                roomId, operationId, failure.getClass().getSimpleName());
    }
}
