package com.sdnah.Ticket_Management_System_.Backend.DTOs;

import java.time.LocalDateTime;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;

public class NotificationDTO {

    private final String id;
    private final String recipientUsername;
    private final String message;
    private final NotificationType type;
    private final LocalDateTime createdAt;
    private final boolean read;

    public NotificationDTO(String id,
                           String recipientUsername,
                           String message,
                           NotificationType type,
                           LocalDateTime createdAt,
                           boolean read) {
        this.id = id;
        this.recipientUsername = recipientUsername;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
        this.read = read;
    }

    public static NotificationDTO fromDomain(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getRecipientUsername(),
                notification.getMessage(),
                notification.getType(),
                notification.getCreatedAt(),
                notification.isRead()
        );
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

    public boolean isRead() {
        return read;
    }
}