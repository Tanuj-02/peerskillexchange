package com.tanuj.peerskillexchange.dto.board;

import java.util.List;

public record BoardKafkaEvent(int schemaVersion, Long roomId, String operationId,
                              String authorId, String type, List<Point> points,
                              String color, Integer width, String targetOperationId) {
    public static BoardKafkaEvent from(Long roomId, String authorId, BoardOperationRequest request) {
        return new BoardKafkaEvent(1, roomId, request.operationId(), authorId,
                request.type(), request.points(), request.color(), request.width(),
                request.targetOperationId());
    }

    public BoardOperationRequest asRequest() {
        return new BoardOperationRequest(operationId, type, points, color, width, targetOperationId);
    }
}
