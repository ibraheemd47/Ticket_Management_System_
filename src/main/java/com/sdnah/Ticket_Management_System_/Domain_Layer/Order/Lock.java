package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Lock {

    @Column(name = "lock_resource_id")
    private String resourceId;

    @Column(name = "lock_buyer_id")
    private String userToken;

    @Column(name = "lock_expires_at")
    private LocalDateTime expiresAt;

    // JPA required
    protected Lock() {
    }

    public Lock(String resourceId, String ownerId, LocalDateTime expiresAt) {
        if (resourceId == null || resourceId.isBlank())
            throw new IllegalArgumentException("resourceId required");
        if (ownerId == null || ownerId.isBlank())
            throw new IllegalArgumentException("ownerId required");
        if (expiresAt == null)
            throw new IllegalArgumentException("expiresAt required");
        this.resourceId = resourceId;
        this.userToken = ownerId;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isHeldBy(String userToken) {
        return this.userToken.equals(userToken);
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getOwnerId() {
        return userToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}