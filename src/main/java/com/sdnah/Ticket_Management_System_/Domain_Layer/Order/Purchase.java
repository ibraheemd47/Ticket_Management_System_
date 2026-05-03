package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Purchase {
    private final UUID purchaseId;
    private final UUID orderId;
    private final String buyerId;
    private final List<OrderItem> items;
    private final BigDecimal totalPrice;
    private final LocalDateTime purchasedAt;
    private final UUID eventId;

    public Purchase(ActiveOrder order) {
        if (order == null)
            throw new IllegalArgumentException("order required");
        this.purchaseId = UUID.randomUUID();
        this.orderId = order.getId();
        this.buyerId = order.getBuyerId();
        this.items = new ArrayList<>(order.getItems());
        this.totalPrice = order.getTotal();
        this.purchasedAt = LocalDateTime.now();
        this.eventId= order.getEventId();
    }

    public UUID getPurchaseId() {
        return purchaseId;
    }

    public UUID getOrderId() {
        return orderId;
    }
    public UUID getEventId() {
        return eventId;
    }

    public String getBuyerId() {
        return buyerId;
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
