package com.tanuj.peerskillexchange.config;

import com.tanuj.peerskillexchange.security.jwt.jwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USERNAME = "ws_username";
    private final jwtService jwtService;

    public JwtHandshakeInterceptor(jwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = null;

        if (request instanceof ServletServerHttpRequest servlet) {
            String qp = servlet.getServletRequest().getParameter("token");
            if (qp != null && !qp.isBlank()) token = qp.trim();
        }

        if (token == null) {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (jwtService.isExpired(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Claims claims = jwtService.verifyTokenandExtractClaims(token);
            String username = claims.getSubject();
            if (username == null || username.isBlank()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(ATTR_USERNAME, username);
            return true;
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}

