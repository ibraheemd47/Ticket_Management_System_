package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {
    private final UUID itemId;
    private final String ticketId; // pre-existing ticket from EVENT aggregate
    private final Long seatId; // null if standing area — display only
    private final UUID areaId;
    private final BigDecimal price;
    private Lock lock; // null after checkout or expiry

    public OrderItem(String ticketId, Long seatId, UUID areaId, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("price must be non-negative");
        this.itemId = UUID.randomUUID();
        this.ticketId = ticketId;
        this.seatId = seatId;
        this.areaId = areaId;
        this.price = price;
        this.lock = null;
    }

    public String getLockResourceId() {
        return ticketId;
    }

    public boolean isSeated() {
        return seatId != null;
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

    public void clearLock() {
        this.lock = null;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public long getSeatId() {
        return seatId;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Lock getLock() {
        return lock;
    }

}
