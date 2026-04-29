package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;
import java.time.LocalDateTime;

public class LockTest {

    private final String resourceId;
    private final String ownerId;
    private final LocalDateTime expiresAt;

    public LockTest(String resourceId, String ownerId, LocalDateTime expiresAt) {

        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("resourceId must not be empty");
        }

        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be empty");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }

        this.resourceId = resourceId;
        this.ownerId = ownerId;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isHeldBy(String buyerId) {
        return ownerId.equals(buyerId);
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}