package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanuj.peerskillexchange.dto.board.BoardOperationResponse;
import com.tanuj.peerskillexchange.dto.board.Point;
import com.tanuj.peerskillexchange.model.BoardOperation;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
public class BoardProjectionAssembler {
    private final ObjectMapper json;

    public BoardProjectionAssembler(ObjectMapper json) { this.json = json; }

    public BoardOperationResponse toResponse(BoardOperation op) {
        List<Point> points = null;
        if (op.getType() == BoardOperation.BoardOperationType.STROKE
                || op.getType() == BoardOperation.BoardOperationType.SHAPE
                || op.getType() == BoardOperation.BoardOperationType.TEXT) {
            try {
                points = json.readValue(op.getPointsJson(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Stored board operation is invalid.", e);
            }
        }
        return new BoardOperationResponse(op.getOperationId(), op.getRoom().getId(),
                op.getSequenceNumber(), op.getAuthor().getId(), op.getAuthor().getName(),
                op.getType().name(), points, op.getColor(), op.getWidth(),
                op.getTargetOperationId(), op.getCreatedAt(), op.getShape(), op.getText());
    }

    public List<BoardOperationResponse> visible(List<BoardOperationResponse> seed,
                                                List<BoardOperationResponse> tail) {
        var strokes = new LinkedHashMap<String, BoardOperationResponse>();
        seed.forEach(stroke -> strokes.put(stroke.operationId(), stroke));
        for (BoardOperationResponse operation : tail) {
            switch (operation.type()) {
                case "STROKE" -> strokes.put(operation.operationId(), operation);
                case "SHAPE", "TEXT" -> strokes.put(operation.operationId(), operation);
                case "ERASE" -> strokes.remove(operation.targetOperationId());
                case "CLEAR" -> strokes.clear();
                default -> throw new IllegalStateException("Stored board operation has unknown type.");
            }
        }
        return List.copyOf(strokes.values());
    }
}
