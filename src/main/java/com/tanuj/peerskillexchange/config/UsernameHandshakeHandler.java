package com.tanuj.peerskillexchange.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class UsernameHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Object username = attributes.get(JwtHandshakeInterceptor.ATTR_USERNAME);
        if (username == null) return null;
        String name = username.toString();
        return () -> name;
    }
}

