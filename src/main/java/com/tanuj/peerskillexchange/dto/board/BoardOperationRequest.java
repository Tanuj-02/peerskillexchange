package com.tanuj.peerskillexchange.dto.board;

import java.util.List;

public record BoardOperationRequest(String operationId, String type, List<Point> points,
                                    String color, Integer width, String targetOperationId,
                                    String shape, String text) {
    public BoardOperationRequest(String operationId, String type, List<Point> points,
                                 String color, Integer width, String targetOperationId) {
        this(operationId, type, points, color, width, targetOperationId, null, null);
    }
}
