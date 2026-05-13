package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class NotificationWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        //private user-specific notifications will be sent to /user/queue/notifications
        config.enableSimpleBroker("/queue");

        //messages sent from client to backend would start with /app
        config.setApplicationDestinationPrefixes("/app");

        //enables user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //frontend connects to this WebSocket endpoint
        registry.addEndpoint("/ws-notifications")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}