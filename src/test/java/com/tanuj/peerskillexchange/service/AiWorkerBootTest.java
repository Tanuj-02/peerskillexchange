package com.tanuj.peerskillexchange.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "ai-worker"})
class AiWorkerBootTest {
    @Autowired AiJobWorker worker;
    @Test void workerStartsWithoutHttpServer() { assertNotNull(worker); }
}
