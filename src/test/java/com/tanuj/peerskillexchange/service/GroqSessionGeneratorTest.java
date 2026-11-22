package com.tanuj.peerskillexchange.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroqSessionGeneratorTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void missingKeyFailsBeforeAnyHttpRequest() {
        var generator = new GroqSessionGenerator(json, "", "test-model", "http://127.0.0.1:1");
        assertEquals("not_configured", assertThrows(GroqSessionGenerator.AiFailure.class,
                () -> generator.generate("skills only")).code());
    }

    @Test
    void acceptsOnlyCompleteThreeQuestionResults() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            assertEquals("Bearer local-test-key", exchange.getRequestHeaders().getFirst("Authorization"));
            String content = "{\"summary\":\"Reviewed Java streams\",\"questions\":[\"What is map?\",\"What is filter?\",\"What is collect?\"]}";
            byte[] body = json.writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var generator = new GroqSessionGenerator(json, "local-test-key", "test-model",
                    "http://127.0.0.1:" + server.getAddress().getPort());
            var result = generator.generate("Java streams");
            assertEquals("Reviewed Java streams", result.summary());
            assertEquals(3, result.questions().size());
        } finally { server.stop(0); }
    }

    @Test
    void malformedProviderResultIsRejected() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] body = "{\"choices\":[{\"message\":{\"content\":\"{\\\"summary\\\":\\\"\\\",\\\"questions\\\":[]}\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var generator = new GroqSessionGenerator(json, "local-test-key", "test-model",
                    "http://127.0.0.1:" + server.getAddress().getPort());
            assertEquals("invalid_output", assertThrows(GroqSessionGenerator.AiFailure.class,
                    () -> generator.generate("Java streams")).code());
        } finally { server.stop(0); }
    }
}
