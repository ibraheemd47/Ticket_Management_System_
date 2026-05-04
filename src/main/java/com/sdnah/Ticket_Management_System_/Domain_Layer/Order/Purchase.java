package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @Column(name = "purchase_id")
    private UUID purchaseId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "buyer_id")
    private String buyerId;

    @Column(name = "event_id")
    private UUID eventId;

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    // JPA required
    protected Purchase() {
    }

    public Purchase(ActiveOrder order) {
        if (order == null)
            throw new IllegalArgumentException("order required");
        this.purchaseId = UUID.randomUUID();
        this.orderId = order.getId();
        this.buyerId = order.getbuyerId();
        this.eventId = order.getEventId();
        this.items = new ArrayList<>(order.getItems());
        this.totalPrice = order.getFinalPrice();
        this.purchasedAt = LocalDateTime.now();
    }

    public UUID getPurchaseId() {
        return purchaseId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getbuyerId() {
        return buyerId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }
}
