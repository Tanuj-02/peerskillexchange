package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.dto.board.RoomResponse;
import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.LearningRoom;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.ExchangeRequestRepository;
import com.tanuj.peerskillexchange.repository.LearningRoomRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import com.tanuj.peerskillexchange.repository.VideoCallRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
public class LearningRoomService {
    private final LearningRoomRepository rooms;
    private final ExchangeRequestRepository exchanges;
    private final UserRepository users;
    private final VideoCallRepository videoCalls;

    public LearningRoomService(LearningRoomRepository rooms, ExchangeRequestRepository exchanges,
                               UserRepository users, VideoCallRepository videoCalls) {
        this.rooms = rooms;
        this.exchanges = exchanges;
        this.users = users;
        this.videoCalls = videoCalls;
    }

    User currentUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Account unavailable."));
    }

    private boolean isMember(User user, LearningRoom room) {
        return room.getOwner().getId().equals(user.getId()) || room.getPeer().getId().equals(user.getId());
    }

    private boolean hasActiveExchange(LearningRoom room) {
        return exchanges.findById(room.getExchangeRequestId())
                .filter(exchange -> exchange.getStatus() == RequestStatus.ACCEPTED)
                .filter(exchange -> {
                    String sender = exchange.getSender().getId();
                    String receiver = exchange.getReceiver().getId();
                    return (sender.equals(room.getOwner().getId()) && receiver.equals(room.getPeer().getId()))
                            || (receiver.equals(room.getOwner().getId()) && sender.equals(room.getPeer().getId()));
                }).isPresent();
    }

    @Transactional
    public RoomResponse create(String username, Long exchangeRequestId) {
        if (exchangeRequestId == null) throw new ResponseStatusException(BAD_REQUEST, "Exchange request is required.");
        User me = currentUser(username);
        ExchangeRequest exchange = exchanges.lockById(exchangeRequestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Exchange request not found."));
        if (exchange.getStatus() != RequestStatus.ACCEPTED) {
            throw new ResponseStatusException(CONFLICT, "The exchange is not accepted.");
        }
        User sender = exchange.getSender();
        User receiver = exchange.getReceiver();
        if (!me.getId().equals(sender.getId()) && !me.getId().equals(receiver.getId())) {
            throw new AccessDeniedException("Room access denied.");
        }
        return rooms.findByExchangeRequestId(exchangeRequestId).map(room -> {
            if (room.getStatus() == LearningRoom.RoomStatus.CLOSED) {
                throw new ResponseStatusException(CONFLICT, "The room is closed.");
            }
            return toResponse(room);
        }).orElseGet(() -> {
            LearningRoom room = new LearningRoom();
            room.setExchangeRequestId(exchangeRequestId);
            room.setOwner(me);
            room.setPeer(me.getId().equals(sender.getId()) ? receiver : sender);
            return toResponse(rooms.saveAndFlush(room));
        });
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> list(String username) {
        User me = currentUser(username);
        return rooms.findForUser(me.getId()).stream()
                .filter(this::hasActiveExchange)
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse join(String username, Long roomId) {
        return toResponse(requireOpenRoom(username, roomId));
    }

    @Transactional(readOnly = true)
    public RoomResponse getForMember(String username, Long roomId) {
        return toResponse(requireMemberRoom(username, roomId));
    }

    @Transactional(readOnly = true)
    public LearningRoom requireMemberRoom(String username, Long roomId) {
        User me = currentUser(username);
        LearningRoom room = rooms.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Room not found."));
        assertMember(me, room);
        return room;
    }

    @Transactional(readOnly = true)
    public LearningRoom requireOpenRoom(String username, Long roomId) {
        LearningRoom room = requireMemberRoom(username, roomId);
        assertOpen(room);
        return room;
    }

    void assertOpenMember(User me, LearningRoom room) {
        assertMember(me, room);
        assertOpen(room);
    }

    void assertMember(User me, LearningRoom room) {
        if (!isMember(me, room) || !hasActiveExchange(room)) {
            throw new AccessDeniedException("Room access denied.");
        }
    }

    private void assertOpen(LearningRoom room) {
        if (room.getStatus() != LearningRoom.RoomStatus.OPEN) {
            throw new ResponseStatusException(CONFLICT, "The room is closed.");
        }
    }

    @Transactional(readOnly = true)
    public void assertAccess(String username, Long roomId) {
        requireOpenRoom(username, roomId);
    }

    @Transactional
    public RoomResponse close(String username, Long roomId) {
        User me = currentUser(username);
        LearningRoom room = rooms.lockById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Room not found."));
        if (!room.getOwner().getId().equals(me.getId())) {
            throw new AccessDeniedException("Only the room owner can close it.");
        }
        if (room.getStatus() == LearningRoom.RoomStatus.CLOSED) return toResponse(room);
        room.setStatus(LearningRoom.RoomStatus.CLOSED);
        room.setClosedAt(LocalDateTime.now());
        videoCalls.findFirstByRoomIdAndEndedAtIsNullOrderByStartedAtDesc(roomId)
                .ifPresent(call -> call.setEndedAt(room.getClosedAt()));
        return toResponse(rooms.save(room));
    }

    @Transactional
    public RoomResponse reopen(String username, Long roomId) {
        User me = currentUser(username);
        LearningRoom room = rooms.lockById(roomId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Room not found."));
        assertMember(me, room);
        if (!room.getOwner().getId().equals(me.getId())) {
            throw new AccessDeniedException("Only the room owner can restart it.");
        }
        if (room.getStatus() == LearningRoom.RoomStatus.OPEN) return toResponse(room);
        room.setStatus(LearningRoom.RoomStatus.OPEN);
        room.setClosedAt(null);
        return toResponse(rooms.save(room));
    }

    public RoomResponse toResponse(LearningRoom room) {
        return new RoomResponse(room.getId(), room.getExchangeRequestId(),
                room.getOwner().getId(), room.getOwner().getName(),
                room.getPeer().getId(), room.getPeer().getName(),
                room.getStatus().name(), room.getSequenceNumber(),
                room.getCreatedAt(), room.getClosedAt());
    }
}
