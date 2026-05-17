package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.logging.Logger;

@Component
public class NotificationConnectionListener {

    private static final Logger logger =
            Logger.getLogger(NotificationConnectionListener.class.getName());

    //private final NotificationSessionRegistry connectedUserRegistry;

    private final Broadcaster broadcaster;
    public NotificationConnectionListener(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();

        if (user == null || sessionId == null) {
            logger.warning("socket connected without user or sessionId");
            return;
        }

        broadcaster.register(user.getName(), sessionId);

        logger.info("Notification socket connected. user="
                + user.getName() + ", sessionId=" + sessionId);
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        broadcaster.unregister(sessionId);

        logger.info("Notification socket disconnected. sessionId=" + sessionId);
    }
}