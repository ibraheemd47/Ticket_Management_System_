package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.time.LocalDateTime;

public class Lock {
    private final String resourceId; // ticketId
    private final String buyerId;
    private final LocalDateTime expiresAt;

    public Lock(String resourceId, String ownerId, LocalDateTime expiresAt) {
        if (resourceId == null || ownerId.isBlank() )
            throw new IllegalArgumentException("resourceId required");
        if (ownerId == null || ownerId.isBlank())
            throw new IllegalArgumentException("ownerId required");
        if (expiresAt == null)
            throw new IllegalArgumentException("expiresAt required");
        this.resourceId = resourceId;
        this.buyerId = ownerId;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isHeldBy(String buyerId) {
        return this.buyerId.equals(buyerId);
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getOwnerId() {
        return buyerId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
