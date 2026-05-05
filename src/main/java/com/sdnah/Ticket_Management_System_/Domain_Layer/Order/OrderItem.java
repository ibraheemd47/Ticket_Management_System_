package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "ticket_id", unique = true, nullable = false)
    private String ticketId;

    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "area_id")
    private UUID areaId;

    @Column(name = "price")
    private BigDecimal price;

    @Embedded
    private Lock lock;

    // JPA required
    protected OrderItem() {
    }

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

    public Long getSeatId() {
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