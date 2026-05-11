package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount;

import java.time.LocalDateTime;
import java.util.UUID;

// =========================================================================
// Generic context object passed to discount rules.
// Designed for extensibility without changing rule APIs.
// =========================================================================
public final class DiscountContext {

    private final int ticketQuantity;
    private final LocalDateTime purchaseTime;
    private final String couponCode;

    // NEW FIX: additional generic fields for future requirements
    private final UUID eventId;
    private final UUID companyId;
    private final double originalPrice;
    private final String ticketType;
    private final Integer buyerId;
    private final Integer buyerAge;

    public DiscountContext(int ticketQuantity,
                           LocalDateTime purchaseTime,
                           String couponCode,
                           UUID eventId,
                           UUID companyId,
                           double originalPrice,
                           String ticketType,
                           Integer buyerId,
                           Integer buyerAge) {

        if (ticketQuantity <= 0) {
            throw new IllegalArgumentException("ticketQuantity must be positive");
        }

        this.ticketQuantity = ticketQuantity;
        this.purchaseTime = purchaseTime != null ? purchaseTime : LocalDateTime.now();
        this.couponCode = couponCode;
        this.eventId = eventId;
        this.companyId = companyId;
        this.originalPrice = originalPrice;
        this.ticketType = ticketType;
        this.buyerId = buyerId;
        this.buyerAge = buyerAge;
    }

    public int getTicketQuantity() {
        return ticketQuantity;
    }

    public LocalDateTime getPurchaseTime() {
        return purchaseTime;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public boolean hasCoupon() {
        return couponCode != null && !couponCode.isBlank();
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public String getTicketType() {
        return ticketType;
    }

    public Integer getBuyerId() {
        return buyerId;
    }

    public Integer getBuyerAge() {
        return buyerAge;
    }
}