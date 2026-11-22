package com.tanuj.peerskillexchange.dto.board;

import java.time.LocalDateTime;
import java.util.List;

public record BoardOperationResponse(String operationId, Long roomId, long sequenceNumber,
                                     String authorId, String authorName, String type,
                                     List<Point> points, String color, Integer width,
                                     String targetOperationId, LocalDateTime createdAt,
                                     String shape, String text) {
    public BoardOperationResponse(String operationId, Long roomId, long sequenceNumber,
                                  String authorId, String authorName, String type,
                                  List<Point> points, String color, Integer width,
                                  String targetOperationId, LocalDateTime createdAt) {
        this(operationId, roomId, sequenceNumber, authorId, authorName, type,
                points, color, width, targetOperationId, createdAt, null, null);
    }
}
