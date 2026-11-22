package com.tanuj.peerskillexchange.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import java.util.Map;

@Configuration
@EnableKafka
public class BoardKafkaConfig {
    @Bean
    CommonErrorHandler boardKafkaErrorHandler() {
        // Never advance the consumer offset past a board event that has not reached the database.
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new FixedBackOff(1000L, FixedBackOff.UNLIMITED_ATTEMPTS));
        handler.setClassifications(Map.of(), true);
        return handler;
    }

    @Bean
    @ConditionalOnProperty(name = "board.kafka.enabled", havingValue = "true")
    NewTopic boardEventsTopic(@Value("${board.kafka.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
