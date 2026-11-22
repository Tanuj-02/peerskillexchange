package com.tanuj.peerskillexchange.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RoomPresenceService {
    private static final Logger log = LoggerFactory.getLogger(RoomPresenceService.class);
    private static final Duration TTL = Duration.ofSeconds(10);
    private final StringRedisTemplate redis;
    private final MeterRegistry metrics;

    public RoomPresenceService(StringRedisTemplate redis, MeterRegistry metrics) {
        this.redis = redis;
        this.metrics = metrics;
    }

    public void touch(Long roomId, String userId) {
        try {
            redis.opsForValue().set("pse:board:" + roomId + ":presence:" + userId, "1", TTL);
        } catch (Exception e) {
            metrics.counter("board.redis.presence.failures").increment();
            log.debug("Board presence Redis unavailable room={} cause={}", roomId,
                    e.getClass().getSimpleName());
        }
    }
}
