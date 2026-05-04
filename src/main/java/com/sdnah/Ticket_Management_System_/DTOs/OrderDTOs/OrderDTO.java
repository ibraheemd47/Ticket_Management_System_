package com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public class OrderDTO {
    private final UUID orderId;
    private final String buyerId;
    private final UUID eventId;
    private final List<OrderItemDTO> items;
    private final LocalDateTime expiresAt;
    private final String status;
    private final BigDecimal originalPrice;
    private final BigDecimal discount;
    private final BigDecimal finalPrice;
    private final String appliedCouponCode;

    public OrderDTO(UUID orderId,
            String buyerId,
            UUID eventId,
            List<OrderItemDTO> items,
            LocalDateTime expiresAt,
            String status,
            BigDecimal originalPrice,
            BigDecimal discount,
            BigDecimal finalPrice,
            String appliedCouponCode) {

        this.orderId = orderId;
        this.buyerId = buyerId;
        this.eventId = eventId;
        this.items = items;
        this.expiresAt = expiresAt;
        this.status = status;
        this.originalPrice = originalPrice;
        this.discount = discount;
        this.finalPrice = finalPrice;
        this.appliedCouponCode = appliedCouponCode;
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

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public String getAppliedCouponCode() {
        return appliedCouponCode;
    }

}
