package com.tanuj.peerskillexchange.service;

import com.tanuj.peerskillexchange.enums.RequestStatus;
import com.tanuj.peerskillexchange.enums.Role;
import com.tanuj.peerskillexchange.model.ExchangeRequest;
import com.tanuj.peerskillexchange.model.User;
import com.tanuj.peerskillexchange.repository.ExchangeRequestRepository;
import com.tanuj.peerskillexchange.repository.UserRepository;
import com.tanuj.peerskillexchange.security.jwt.jwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LearningRoomWebSocketIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired ExchangeRequestRepository exchanges;
    @Autowired LearningRoomService rooms;
    @Autowired jwtService jwt;
    @Autowired SimpUserRegistry registry;

    private User user(String id, String name) {
        User user = new User();
        user.setId(id); user.setUsername(name); user.setName(name);
        user.setEmail(name + "@example.test"); user.setPassword("test-only-hash");
        user.setRole(Role.USER); user.setEmailVerified(true);
        return users.save(user);
    }

    private HttpResponse<String> request(String username, String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + jwt.generateToken(username, "USER"));
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void twoClientsReceiveLiveUpdatesAndReconnectBySequence() throws Exception {
        User alice = user("wsa001", "wsa");
        User bob = user("wsb001", "wsb");
        user("wsc001", "wsc");
        ExchangeRequest exchange = new ExchangeRequest();
        exchange.setSender(alice); exchange.setReceiver(bob);
        exchange.setStatus(RequestStatus.ACCEPTED);
        exchange = exchanges.save(exchange);
        long roomId = rooms.create("wsa", exchange.getId()).id();

        try (FrameSocket first = new FrameSocket(port, jwt.generateToken("wsa", "USER"));
             FrameSocket second = new FrameSocket(port, jwt.generateToken("wsb", "USER"));
             FrameSocket stranger = new FrameSocket(port, jwt.generateToken("wsc", "USER"));
             FrameSocket strangerSender = new FrameSocket(port, jwt.generateToken("wsc", "USER"))) {
            first.subscribe(roomId, "first");
            second.subscribe(roomId, "second");
            awaitSubscription("wsa", roomId);
            awaitSubscription("wsb", roomId);
            stranger.send("SUBSCRIBE\nid:stranger\ndestination:/topic/rooms/" + roomId + "\n\n\0");
            strangerSender.send("SEND\ndestination:/app/rooms.cursor/" + roomId
                    + "\ncontent-type:application/json\n\n{\"x\":0.5,\"y\":0.5,\"active\":true}\0");

            String firstId = UUID.randomUUID().toString();
            String stroke = "{\"operationId\":\"" + firstId + "\",\"type\":\"STROKE\","
                    + "\"points\":[{\"x\":0.1,\"y\":0.2},{\"x\":0.3,\"y\":0.4}],"
                    + "\"color\":\"#112233\",\"width\":3}";
            assertEquals(200, request("wsa", "POST", "/api/rooms/" + roomId + "/operations", stroke).statusCode());
            assertTrue(first.next("MESSAGE").contains(firstId));
            assertTrue(second.next("MESSAGE").contains(firstId));
            assertEquals(409, request("wsa", "POST", "/api/rooms/" + roomId + "/operations", stroke).statusCode());
            assertEquals(403, request("wsc", "GET", "/api/rooms/" + roomId + "/operations?after=0", null).statusCode());

            second.close();
            String secondId = UUID.randomUUID().toString();
            String clear = "{\"operationId\":\"" + secondId + "\",\"type\":\"CLEAR\"}";
            assertEquals(200, request("wsa", "POST", "/api/rooms/" + roomId + "/operations", clear).statusCode());
            assertTrue(first.next("MESSAGE").contains(secondId));
            try (FrameSocket reconnected = new FrameSocket(port, jwt.generateToken("wsb", "USER"))) {
                reconnected.subscribe(roomId, "rejoined");
                awaitSubscription("wsb", roomId);
                var catchUp = request("wsb", "GET", "/api/rooms/" + roomId + "/operations?after=1", null);
                assertEquals(200, catchUp.statusCode());
                assertTrue(catchUp.body().contains(secondId));
                assertFalse(catchUp.body().contains(firstId));
            }
            assertFalse(stranger.sawMessage());
        }
    }

    private void awaitSubscription(String username, long roomId) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var user = registry.getUser(username);
            if (user != null && user.getSessions().stream().flatMap(session -> session.getSubscriptions().stream())
                    .anyMatch(subscription -> subscription.getDestination().equals("/topic/rooms/" + roomId))) return;
            Thread.sleep(30);
        }
        fail("Room subscription was not registered for " + username);
    }

    private static class FrameSocket implements AutoCloseable, WebSocket.Listener {
        private final WebSocket socket;
        private final LinkedBlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final StringBuilder partial = new StringBuilder();

        FrameSocket(int port, String token) throws Exception {
            socket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
                    URI.create("ws://localhost:" + port + "/ws/websocket?token=" + token), this).get(5, TimeUnit.SECONDS);
            send("CONNECT\naccept-version:1.2\nhost:localhost\nheart-beat:0,0\n\n\0");
            assertNotNull(next("CONNECTED"));
        }

        void subscribe(long roomId, String id) throws Exception {
            send("SUBSCRIBE\nid:" + id + "\ndestination:/topic/rooms/" + roomId + "\n\n\0");
        }

        void send(String frame) {
            socket.sendText(frame, true).join();
        }

        String next(String prefix) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                String frame = frames.poll(200, TimeUnit.MILLISECONDS);
                if (frame != null && frame.startsWith(prefix)) return frame;
            }
            fail("Did not receive STOMP " + prefix);
            return null;
        }

        boolean sawMessage() {
            return frames.stream().anyMatch(frame -> frame.startsWith("MESSAGE"));
        }

        @Override public void onOpen(WebSocket webSocket) { webSocket.request(1); }
        @Override public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            synchronized (partial) {
                partial.append(data);
                int end;
                while ((end = partial.indexOf("\0")) >= 0) {
                    frames.add(partial.substring(0, end).stripLeading());
                    partial.delete(0, end + 1);
                }
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }
        @Override public void close() { socket.sendClose(WebSocket.NORMAL_CLOSURE, "done"); }
    }
}
