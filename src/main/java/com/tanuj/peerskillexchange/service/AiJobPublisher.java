package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.model.AiSessionJob;
import com.tanuj.peerskillexchange.repository.AiSessionJobRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
class AiJobConfig {
    @Bean
    @ConditionalOnProperty(name = "ai.kafka.enabled", havingValue = "true", matchIfMissing = true)
    NewTopic aiJobsTopic(@Value("${ai.kafka.topic:pse-ai-jobs-v1}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}

@Component
@Profile("!ai-worker")
@ConditionalOnProperty(name = "ai.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AiJobPublisher {
    private static final Logger log = LoggerFactory.getLogger(AiJobPublisher.class);
    private final AiSessionJobRepository jobs;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    public AiJobPublisher(AiSessionJobRepository jobs, KafkaTemplate<String, String> kafka,
                          @Value("${ai.kafka.topic:pse-ai-jobs-v1}") String topic) {
        this.jobs = jobs; this.kafka = kafka; this.topic = topic;
    }
    @Scheduled(fixedDelayString = "${ai.publisher.interval-ms:3000}")
    @Transactional
    public void publish() {
        for (AiSessionJob candidate : jobs.findTop20ByStatusAndPublishedAtIsNullOrderByIdAsc(AiSessionJob.Status.QUEUED)) {
            AiSessionJob job = jobs.lockById(candidate.getId()).orElseThrow();
            if (job.getPublishedAt() != null || job.getStatus() != AiSessionJob.Status.QUEUED) continue;
            try {
                kafka.send(topic, job.getRoom().getId().toString(), job.getId().toString()).get(7, TimeUnit.SECONDS);
                job.setPublishedAt(LocalDateTime.now());
                jobs.save(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("AI job publication interrupted job={}", job.getId());
                return;
            } catch (Exception e) {
                // The database row remains queued; the next tick retries publication.
                log.warn("AI job publication delayed job={} cause={}", job.getId(), e.getClass().getSimpleName());
                return;
            }
        }
    }
}
