package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class Broadcaster {

    private static final String NOTIFICATION_DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationSessionRegistry  connectedUserRegistry;

    public Broadcaster(SimpMessagingTemplate messagingTemplate,
                                   NotificationSessionRegistry  connectedUserRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.connectedUserRegistry = connectedUserRegistry;
    }

    public void register(String memberId, String sessionId) {
        connectedUserRegistry.register(memberId, sessionId);
    }

    public void unregister(String sessionId) {
        connectedUserRegistry.unregisterSession(sessionId);
    }

    public boolean isConnected(String memberId) {
        return connectedUserRegistry.isConnected(memberId);
    }

    public int getSessionCount(String memberId) {
        return connectedUserRegistry.getSessionCount(memberId);
    }

    public boolean broadcast(String recipientMemberId, NotificationDTO notification) {
        if (recipientMemberId == null || recipientMemberId.isBlank()) {
            throw new IllegalArgumentException("recipientMemberId cannot be null or blank");
        }

        if (notification == null) {
            throw new IllegalArgumentException("notification cannot be null");
        }

        String cleanRecipient = recipientMemberId.trim();
        if(cleanRecipient==null||cleanRecipient.isEmpty()) {
            throw new IllegalArgumentException("recipientMemberId cannot be empty or whitespace");
        }

        if (!connectedUserRegistry.isConnected(cleanRecipient)) {
            return false;
        }

        messagingTemplate.convertAndSendToUser(
                cleanRecipient,
                NOTIFICATION_DESTINATION,
                notification
        );

        return true;
    }
}