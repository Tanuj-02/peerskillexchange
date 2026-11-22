package com.tanuj.peerskillexchange.dto.board;

import java.util.List;

public record BoardKafkaEvent(int schemaVersion, Long roomId, String operationId,
                              String authorId, String type, List<Point> points,
                              String color, Integer width, String targetOperationId,
                              String shape, String text) {
    public BoardKafkaEvent(int schemaVersion, Long roomId, String operationId,
                           String authorId, String type, List<Point> points,
                           String color, Integer width, String targetOperationId) {
        this(schemaVersion, roomId, operationId, authorId, type, points, color, width,
                targetOperationId, null, null);
    }
    public static BoardKafkaEvent from(Long roomId, String authorId, BoardOperationRequest request) {
        return new BoardKafkaEvent(1, roomId, request.operationId(), authorId,
                request.type(), request.points(), request.color(), request.width(),
                request.targetOperationId(), request.shape(), request.text());
    }

    public BoardOperationRequest asRequest() {
        return new BoardOperationRequest(operationId, type, points, color, width, targetOperationId, shape, text);
    }
}
