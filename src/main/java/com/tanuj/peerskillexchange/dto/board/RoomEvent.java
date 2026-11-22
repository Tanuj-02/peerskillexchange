package com.tanuj.peerskillexchange.dto.board;

public record RoomEvent(String type, BoardOperationResponse operation,
                        RoomCursor cursor, RoomResponse room) {
    public static RoomEvent operation(BoardOperationResponse operation) {
        return new RoomEvent("OPERATION", operation, null, null);
    }
    public static RoomEvent cursor(RoomCursor cursor) {
        return new RoomEvent("CURSOR", null, cursor, null);
    }
    public static RoomEvent closed(RoomResponse room) {
        return new RoomEvent("ROOM_CLOSED", null, null, room);
    }
    public static RoomEvent reopened(RoomResponse room) {
        return new RoomEvent("ROOM_REOPENED", null, null, room);
    }
}
