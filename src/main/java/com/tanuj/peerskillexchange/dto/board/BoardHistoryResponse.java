package com.tanuj.peerskillexchange.dto.board;

import java.util.List;

public record BoardHistoryResponse(List<BoardOperationResponse> operations,
                                   long latestSequence, boolean hasMore) {}
