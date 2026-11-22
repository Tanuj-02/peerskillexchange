package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.board.BoardOperationRequest;
import com.tanuj.peerskillexchange.dto.board.BoardOperationResponse;
import com.tanuj.peerskillexchange.repository.BoardOperationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class BoardPipelineService {
    private final BoardService board;
    private final BoardKafkaPublisher publisher;
    private final BoardOperationRepository operations;
    private final boolean kafkaEnabled;
    private final BoardCompletionNotifier completions;

    public BoardPipelineService(BoardService board, BoardKafkaPublisher publisher,
                                BoardOperationRepository operations,
                                @Value("${board.kafka.enabled:true}") boolean kafkaEnabled,
                                BoardCompletionNotifier completions) {
        this.board = board;
        this.publisher = publisher;
        this.operations = operations;
        this.kafkaEnabled = kafkaEnabled;
        this.completions = completions;
    }

    public BoardOperationResponse append(String username, Long roomId, BoardOperationRequest input) {
        if (!kafkaEnabled) return board.append(username, roomId, input);
        var completion = completions.register(roomId, input.operationId());
        try {
            var receipt = publisher.publish(username, roomId, input);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                try {
                    return completion.get(100, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignored) {
                    // A different service instance may own the Kafka consumer.
                    var applied = operations.findByRoomIdAndOperationId(roomId, receipt.operationId());
                    if (applied.isPresent()) return board.toResponse(applied.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (java.util.concurrent.ExecutionException e) {
                    break;
                }
            }
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Operation is durable but the board is still recovering. Retry with the same operation ID.");
        } finally {
            completions.remove(roomId, input.operationId(), completion);
        }
    }

    public boolean kafkaEnabled() { return kafkaEnabled; }
}
