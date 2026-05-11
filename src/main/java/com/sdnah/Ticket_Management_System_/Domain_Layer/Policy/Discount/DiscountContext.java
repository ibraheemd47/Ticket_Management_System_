package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable value object passed to every DiscountRule.
 *
 * Fields:
 *   - ticketQuantity  → QuantityConditionalDiscountRule
 *   - purchaseTime    → DateRangeDiscountRule
 *   - couponCode      → CouponDiscountRule
 *   - originalPrice   → price-based discount rules
 *   - eventId         → event-scoped discount rules
 */
public final class DiscountContext {

    private final int           ticketQuantity;
    private final LocalDateTime purchaseTime;
    private final String        couponCode;
    private final double        originalPrice;
    private final UUID          eventId;

    // ── Full constructor ───────────────────────────────────────────────────────

    public DiscountContext(int ticketQuantity,
                           LocalDateTime purchaseTime,
                           String couponCode,
                           double originalPrice,
                           UUID eventId) {
        if (ticketQuantity <= 0)
            throw new IllegalArgumentException("ticketQuantity must be positive");
        if (originalPrice < 0)
            throw new IllegalArgumentException("originalPrice must be non-negative");
        this.ticketQuantity = ticketQuantity;
        this.purchaseTime   = purchaseTime != null ? purchaseTime : LocalDateTime.now();
        this.couponCode     = couponCode;
        this.originalPrice  = originalPrice;
        this.eventId        = eventId;
    }

    // ── Convenience: no coupon, no eventId ────────────────────────────────────

    public DiscountContext(int ticketQuantity, LocalDateTime purchaseTime) {
        this(ticketQuantity, purchaseTime, null, 0.0, null);
    }

    // ── Convenience: with coupon, no eventId ──────────────────────────────────

    public DiscountContext(int ticketQuantity, LocalDateTime purchaseTime, String couponCode) {
        this(ticketQuantity, purchaseTime, couponCode, 0.0, null);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int           getTicketQuantity() { return ticketQuantity; }
    public LocalDateTime getPurchaseTime()   { return purchaseTime; }
    public String        getCouponCode()     { return couponCode; }
    public boolean       hasCoupon()         { return couponCode != null && !couponCode.isBlank(); }
    public double        getOriginalPrice()  { return originalPrice; }
    public UUID          getEventId()        { return eventId; }
}