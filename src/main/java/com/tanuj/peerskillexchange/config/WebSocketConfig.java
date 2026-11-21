package com.tanuj.peerskillexchange.config;

import com.tanuj.peerskillexchange.security.jwt.jwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final jwtService jwtService;
    private final ChatSubscriptionInterceptor chatSubscriptionInterceptor;
    private final String frontendOrigin;

    public WebSocketConfig(jwtService jwtService, ChatSubscriptionInterceptor chatSubscriptionInterceptor,
                           @Value("${FRONTEND_URL}") String frontendOrigin) {
        this.jwtService = jwtService;
        this.chatSubscriptionInterceptor = chatSubscriptionInterceptor;
        this.frontendOrigin = frontendOrigin.replaceAll("/+$", "");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatSubscriptionInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:3000", frontendOrigin)
                .addInterceptors(new JwtHandshakeInterceptor(jwtService))
                .setHandshakeHandler(new UsernameHandshakeHandler())
                .withSockJS();
    }
}

