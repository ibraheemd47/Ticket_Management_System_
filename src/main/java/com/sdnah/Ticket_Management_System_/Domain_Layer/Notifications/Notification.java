package com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Notification {

    private final String id;
    private final String recipientUsername;
    private final String message;
    private final NotificationType type;
    private final LocalDateTime createdAt;

    private NotificationStatus status;
    private boolean active;

    public Notification(String recipientUsername, String message, NotificationType type) {
        this(UUID.randomUUID().toString(), recipientUsername, message, type,
                LocalDateTime.now(), NotificationStatus.PENDING, true);
    }

    public Notification(String id,
                        String recipientUsername,
                        String message,
                        NotificationType type,
                        LocalDateTime createdAt,
                        NotificationStatus status,
                        boolean active) {

        validateId(id);
        validateRecipientUsername(recipientUsername);
        validateMessage(message);
        validateType(type);
        validateCreatedAt(createdAt);
        validateStatus(status);

        this.id = id;
        this.recipientUsername = recipientUsername.trim();
        this.message = message.trim();
        this.type = type;
        this.createdAt = createdAt;
        this.status = status;
        this.active = active;
    }

    public void markAsDelivered() {
        if (!active) {
            throw new IllegalStateException("Cannot deliver an inactive notification.");
        }
        if (status == NotificationStatus.READ) {
            return;
        }
        status = NotificationStatus.DELIVERED;
    }

    public void markAsRead() {
        if (!active) {
            throw new IllegalStateException("Cannot read an inactive notification.");
        }
        status = NotificationStatus.READ;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isUnread() {
        return status != NotificationStatus.READ;
    }

    public boolean belongsTo(String username) {
        return recipientUsername.equals(username);
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Notification id cannot be null or blank.");
        }
    }

    private void validateRecipientUsername(String recipientUsername) {
        if (recipientUsername == null || recipientUsername.isBlank()) {
            throw new IllegalArgumentException("Recipient username cannot be null or blank.");
        }
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Notification message cannot be null or blank.");
        }
    }

    private void validateType(NotificationType type) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null.");
        }
    }

    private void validateCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Creation time cannot be null.");
        }
    }

    private void validateStatus(NotificationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Notification status cannot be null.");
        }
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

    public NotificationStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
