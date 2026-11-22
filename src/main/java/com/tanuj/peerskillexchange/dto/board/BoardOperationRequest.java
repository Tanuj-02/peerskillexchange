package com.tanuj.peerskillexchange.dto.board;

import java.util.List;

public record BoardOperationRequest(String operationId, String type, List<Point> points,
                                    String color, Integer width, String targetOperationId) {}
