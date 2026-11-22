package com.tanuj.peerskillexchange.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AiJobWorkerTest {
    private final ConsumerRecord<String, String> record = new ConsumerRecord<>("ai", 0, 0, "9", "5");

    @Test
    void duplicateDeliveryDoesNotCallProvider() {
        AiSessionService jobs = mock(AiSessionService.class);
        AiSessionGenerator generator = mock(AiSessionGenerator.class);
        new AiJobWorker(jobs, generator).onJob(record);
        verify(jobs).claim(5L);
        verifyNoInteractions(generator);
    }

    @Test
    void missingProviderKeyTerminatesWithoutRetrying() {
        AiSessionService jobs = mock(AiSessionService.class);
        AiSessionGenerator generator = mock(AiSessionGenerator.class);
        when(jobs.claim(5L)).thenReturn(true);
        when(jobs.context(5L)).thenReturn("Java");
        when(generator.generate("Java")).thenThrow(new GroqSessionGenerator.AiFailure("not_configured"));
        new AiJobWorker(jobs, generator).onJob(record);
        verify(jobs).failedAttempt(5L, "not_configured");
        verify(generator, times(1)).generate("Java");
    }
}
