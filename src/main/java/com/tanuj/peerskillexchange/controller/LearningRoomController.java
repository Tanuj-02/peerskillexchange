package com.tanuj.peerskillexchange.controller;

import com.tanuj.peerskillexchange.dto.board.*;
import com.tanuj.peerskillexchange.service.BoardService;
import com.tanuj.peerskillexchange.service.BoardPipelineService;
import com.tanuj.peerskillexchange.service.BoardProjectionService;
import com.tanuj.peerskillexchange.service.LearningRoomService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class LearningRoomController {
    private final LearningRoomService rooms;
    private final BoardService board;
    private final BoardPipelineService pipeline;
    private final BoardProjectionService projection;
    private final SimpMessagingTemplate messaging;

    public LearningRoomController(LearningRoomService rooms, BoardService board,
                                  BoardPipelineService pipeline, BoardProjectionService projection,
                                  SimpMessagingTemplate messaging) {
        this.rooms = rooms;
        this.board = board;
        this.pipeline = pipeline;
        this.projection = projection;
        this.messaging = messaging;
    }

    private String username() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public record CreateRoomRequest(Long exchangeRequestId) {}

    @PostMapping
    public RoomResponse create(@RequestBody CreateRoomRequest request) {
        return rooms.create(username(), request.exchangeRequestId());
    }

    @GetMapping
    public List<RoomResponse> list() {
        return rooms.list(username());
    }

    @GetMapping("/{roomId}")
    public RoomResponse get(@PathVariable Long roomId) {
        return rooms.getForMember(username(), roomId);
    }

    @PostMapping("/{roomId}/join")
    public RoomResponse join(@PathVariable Long roomId) {
        return rooms.join(username(), roomId);
    }

    @PostMapping("/{roomId}/close")
    public RoomResponse close(@PathVariable Long roomId) {
        RoomResponse closed = rooms.close(username(), roomId);
        messaging.convertAndSend("/topic/rooms/" + roomId, RoomEvent.closed(closed));
        return closed;
    }

    @GetMapping("/{roomId}/operations")
    public BoardHistoryResponse history(@PathVariable Long roomId,
                                        @RequestParam(defaultValue = "0") long after) {
        return board.history(username(), roomId, after);
    }

    @GetMapping("/{roomId}/board")
    public BoardProjectionResponse board(@PathVariable Long roomId) {
        return projection.get(username(), roomId);
    }

    @PostMapping("/{roomId}/operations")
    public BoardOperationResponse append(@PathVariable Long roomId,
                                         @RequestBody BoardOperationRequest request) {
        BoardOperationResponse operation = pipeline.append(username(), roomId, request);
        if (!pipeline.kafkaEnabled()) {
            messaging.convertAndSend("/topic/rooms/" + roomId, RoomEvent.operation(operation));
        }
        return operation;
    }
}
