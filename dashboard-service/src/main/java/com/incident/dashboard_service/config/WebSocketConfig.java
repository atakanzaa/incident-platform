package com.incident.dashboard_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}")
    private String allowedOriginsString;

    @Value("${websocket.endpoint:/ws}")
    private String endpoint;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory broker for topics
        config.enableSimpleBroker("/topic", "/queue");
        
        // Set application destination prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = allowedOriginsString.split(",");
        
        // Register STOMP endpoint
        registry.addEndpoint(endpoint)
                .setAllowedOrigins(allowedOrigins)
                .withSockJS(); // Enable SockJS fallback options
        
        // Also register without SockJS for native WebSocket clients
        registry.addEndpoint(endpoint)
                .setAllowedOrigins(allowedOrigins);
    }
} 