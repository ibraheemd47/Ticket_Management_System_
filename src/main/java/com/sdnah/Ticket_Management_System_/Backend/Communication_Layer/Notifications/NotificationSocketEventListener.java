package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.logging.Logger;

@Component
public class NotificationSocketEventListener {

    private static final Logger logger =
            Logger.getLogger(NotificationSocketEventListener.class.getName());

    private final ConnectedUserRegistry connectedUserRegistry;

    public NotificationSocketEventListener(ConnectedUserRegistry connectedUserRegistry) {
        this.connectedUserRegistry = connectedUserRegistry;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();

        if (user == null || sessionId == null) {
            logger.warning("WebSocket connected without user or sessionId");
            return;
        }

        connectedUserRegistry.register(user.getName(), sessionId);

        logger.info("Notification WebSocket connected. user="
                + user.getName() + ", sessionId=" + sessionId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        connectedUserRegistry.unregisterSession(sessionId);

        logger.info("Notification WebSocket disconnected. sessionId=" + sessionId);
    }
}