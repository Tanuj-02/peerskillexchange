package com.tanuj.peerskillexchange.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("ai-worker")
public class AiJobWorker {
    private final AiSessionService jobs;
    private final AiSessionGenerator generator;
    public AiJobWorker(AiSessionService jobs, AiSessionGenerator generator) {
        this.jobs = jobs; this.generator = generator;
    }
    @KafkaListener(topics = "${ai.kafka.topic:pse-ai-jobs-v1}", groupId = "${ai.kafka.group:pse-ai-worker-v1}",
                   autoStartup = "${ai.kafka.enabled:true}")
    public void onJob(ConsumerRecord<String, String> record) {
        Long jobId = Long.valueOf(record.value());
        int retries = 0;
        while (jobs.claim(jobId)) {
            try {
                jobs.succeeded(jobId, generator.generate(jobs.context(jobId)));
                return;
            } catch (GroqSessionGenerator.AiFailure e) {
                if (!jobs.failedAttempt(jobId, e.code())) return;
            } catch (Exception e) {
                if (!jobs.failedAttempt(jobId, "provider_unavailable")) return;
            }
            try { Thread.sleep(Math.min(2000L, ++retries * 1000L)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("AI worker interrupted", e); }
        }
    }
}
