package com.sdnah.Ticket_Management_System_.DTOs;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.NotificationType;

import java.time.LocalDateTime;

public class NotificationDTO {

    private final String id;
    private final String recipientUsername;
    private final String message;
    private final NotificationType type;
    private final LocalDateTime createdAt;

    public NotificationDTO(String id, String recipientUsername, String message, NotificationType type, LocalDateTime createdAt) {
        this.id = id;
        this.recipientUsername = recipientUsername;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
    }

    public static NotificationDTO fromDomain(Notification notification) {
        return new NotificationDTO(notification.getId(), notification.getRecipientUsername(), notification.getMessage(), 
        notification.getType(), notification.getCreatedAt());
    }

    public String getId() {
        return id;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
