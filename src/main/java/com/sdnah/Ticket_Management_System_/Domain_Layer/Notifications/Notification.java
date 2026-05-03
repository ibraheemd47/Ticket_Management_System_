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

    public Notification(String recipientUsername, String message, NotificationType type) {
        this(UUID.randomUUID().toString(), recipientUsername, message, type, LocalDateTime.now());
    }

    public Notification(String id, String recipientUsername, String message, NotificationType type, LocalDateTime createdAt) {
        validateId(id);
        validateRecipientUsername(recipientUsername);
        validateMessage(message);
        validateType(type);
        validateCreatedAt(createdAt);
        this.id = id;
        this.recipientUsername = recipientUsername.trim();
        this.message = message.trim();
        this.type = type;
        this.createdAt = createdAt;
    }

    /*
     * Helper used by the repository/service layer to retrieve notifications
     * for a specific member.
     */
    public boolean belongsTo(String username) {
        return recipientUsername.equals(username);
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Notification id cannot be null or blank.");
        }
    }

    /*
     * Supports notification ownership by a specific recipient.
     * This matches the use-case idea that notifications are associated with a member.
     */
    private void validateRecipientUsername(String recipientUsername) {
        if (recipientUsername == null || recipientUsername.isBlank()) {
            throw new IllegalArgumentException("Recipient username cannot be null or blank.");
        }
    }

    /*
     * Supports negative test scenarios:
     * invalid/incomplete notification data must be rejected.
     */
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

    /*
     * Entity identity is based on notification id.
     */
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