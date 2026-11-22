package com.tanuj.peerskillexchange.dto.board;

import java.time.LocalDateTime;

public record RoomCursor(String userId, String userName, double x, double y, boolean active,
                         LocalDateTime timestamp) {}
