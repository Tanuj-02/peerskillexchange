package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.board.BoardOperationResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BoardCompletionNotifier {
    private final ConcurrentHashMap<String, CompletableFuture<BoardOperationResponse>> waiting = new ConcurrentHashMap<>();

    private String key(Long roomId, String operationId) { return roomId + ":" + operationId; }

    public CompletableFuture<BoardOperationResponse> register(Long roomId, String operationId) {
        return waiting.computeIfAbsent(key(roomId, operationId), ignored -> new CompletableFuture<>());
    }

    public void completed(Long roomId, String operationId, BoardOperationResponse operation) {
        var future = waiting.get(key(roomId, operationId));
        if (future != null) future.complete(operation);
    }

    public void remove(Long roomId, String operationId, CompletableFuture<BoardOperationResponse> future) {
        waiting.remove(key(roomId, operationId), future);
    }
}
