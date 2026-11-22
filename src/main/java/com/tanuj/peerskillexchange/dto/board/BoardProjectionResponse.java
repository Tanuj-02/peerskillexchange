package com.tanuj.peerskillexchange.dto.board;

import java.util.List;

public record BoardProjectionResponse(long sequenceNumber, long kafkaOffset,
                                      List<BoardOperationResponse> strokes, String source) {}
