package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private String id;

    @Column(nullable = false)
    private String recipientUsername;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean read;

    protected Notification() {
        // required by JPA
    }

    public Notification(String recipientUsername, String message, NotificationType type) {
        this(UUID.randomUUID().toString(), recipientUsername, message, type, LocalDateTime.now(), false);
    }

    public Notification(String id,
                        String recipientUsername,
                        String message,
                        NotificationType type,
                        LocalDateTime createdAt,
                        boolean read) {
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
        this.read = read;
    }

    public boolean belongsTo(String username) {
        return username != null && recipientUsername.equals(username.trim());
    }

    public void markAsRead() {
        this.read = true;
    }

    public boolean isRead() {
        return read;
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