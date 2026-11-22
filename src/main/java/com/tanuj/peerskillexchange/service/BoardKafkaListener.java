package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanuj.peerskillexchange.dto.board.BoardKafkaEvent;
import com.tanuj.peerskillexchange.dto.board.RoomEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("!ai-worker")
public class BoardKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(BoardKafkaListener.class);
    private final BoardService board;
    private final ObjectMapper json;
    private final SimpMessagingTemplate messaging;
    private final MeterRegistry metrics;
    private final BoardCompletionNotifier completions;

    public BoardKafkaListener(BoardService board, ObjectMapper json,
                              SimpMessagingTemplate messaging, MeterRegistry metrics,
                              BoardCompletionNotifier completions) {
        this.board = board;
        this.json = json;
        this.messaging = messaging;
        this.metrics = metrics;
        this.completions = completions;
    }

    @KafkaListener(topics = "${board.kafka.topic}", autoStartup = "${board.kafka.enabled:true}")
    public void onEvent(ConsumerRecord<String, String> record) throws Exception {
        BoardKafkaEvent event = json.readValue(record.value(), BoardKafkaEvent.class);
        if (!record.key().equals(event.roomId().toString())) {
            throw new IllegalArgumentException("Board event room key mismatch.");
        }
        var applied = board.applyPublishedEvent(event, record.partition(), record.offset());
        if (applied == null) {
            metrics.counter("board.kafka.consumer.duplicates").increment();
        } else {
            completions.completed(event.roomId(), event.operationId(), applied);
            metrics.counter("board.kafka.consumer.applied").increment();
            messaging.convertAndSend("/topic/rooms/" + event.roomId(), RoomEvent.operation(applied));
        }
        log.debug("Board event projected room={} partition={} offset={}",
                event.roomId(), record.partition(), record.offset());
    }
}
