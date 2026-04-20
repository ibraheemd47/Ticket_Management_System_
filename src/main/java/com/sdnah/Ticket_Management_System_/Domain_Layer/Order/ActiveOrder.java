package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActiveOrder {
    //private static int counter = 0;
    private final UUID id;
    private final String buyerId;
    private final int eventId;
    private final List<OrderItem> items;
    private final LocalDateTime expiresAt;
    private Status status;
    private long version;   // optimistic locking
    public enum Status { ACTIVE, EXPIRED, COMPLETED, CANCELLED }

    public ActiveOrder(String buyerId, int eventId, int ttlMinutes) {
        if (buyerId == null || buyerId.isBlank())
            throw new IllegalArgumentException("buyerId required");
        if (ttlMinutes <= 0)
            throw new IllegalArgumentException("ttlMinutes must be positive");
        this.id = UUID.randomUUID();
        this.buyerId = buyerId;
        this.eventId = eventId;
        this.items = new ArrayList<>();
        this.expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);
        this.status = Status.ACTIVE;
        this.version = 0;
    }
    public void addItem(OrderItem item) {}//////TODO
    public void removeItem(UUID itemId) {}///////TODO
    public boolean isExpired() {return false ;}//TODO
    public boolean isOwnedBy(String buyerId) {return false ;}/////TODO
    public BigDecimal getTotal() {return null;}/////TODO
    public void markCompleted() {
        this.status = Status.COMPLETED;
    }
    public void markExpired() {
        this.status = Status.EXPIRED;
    }
    public void markCancelled() { 
        this.status = Status.CANCELLED;
    }
    public UUID getId(){ 
        return id;
    }
    public String getBuyerId(){ 
        return buyerId;
    }
    public int getEventId(){ 
        return eventId; 
    }
    public List<OrderItem> getItems(){
        return items; 
    }
    public LocalDateTime getExpiresAt() { 
        return expiresAt; 
    }
    public Status getStatus() { 
        return status; 
    }
    public long getVersion() { 
        return version; 
    }
    public void setVersion(long v) { 
        this.version = v; 
    }

    
}
