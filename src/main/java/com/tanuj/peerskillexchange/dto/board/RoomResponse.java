package com.tanuj.peerskillexchange.dto.board;

import java.time.LocalDateTime;

public record RoomResponse(Long id, Long exchangeRequestId, String ownerId, String ownerName,
                           String peerId, String peerName, String status, long latestSequence,
                           LocalDateTime createdAt, LocalDateTime closedAt) {}
