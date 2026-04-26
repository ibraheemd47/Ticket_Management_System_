package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActiveOrder {
    private final UUID id;
    private final String buyerId;
    private final UUID eventId;
    private final List<OrderItem> items;
    private final LocalDateTime expiresAt;
    private Status status;
    private long version; // optimistic locking
    private BigDecimal discount = BigDecimal.ZERO;
    private String appliedCouponCode;
    private BigDecimal finalPrice = null; // updated every time PolicyService returns a result

    public enum Status {
        ACTIVE, EXPIRED, COMPLETED, CANCELLED
    }

    public ActiveOrder(String buyerId, UUID eventId, int ttlMinutes) {
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

    // public void addItem(OrderItem item) {
    // if (item == null)
    // throw new IllegalArgumentException("item required");
    // if (status != Status.ACTIVE)
    // throw new IllegalStateException("Order is not active");
    // if (isExpired())
    // throw new IllegalStateException("Order has expired");
    // items.add(item);
    // }
    public OrderItem addTicket(String ticketId, Long seatId, UUID areaId,
            BigDecimal price, Lock lock) {
        if (status != Status.ACTIVE)
            throw new IllegalStateException("Order is not active");
        if (isExpired())
            throw new IllegalStateException("Order has expired");
        OrderItem item = new OrderItem(ticketId, seatId, areaId, price);
        item.setLock(lock);
        items.add(item);
        return item;
    }

    public void removeItem(UUID itemId) {
        if (status != Status.ACTIVE)
            throw new IllegalStateException("Order is not active");
        if (isExpired())
            throw new IllegalStateException("Order has expired");
        boolean removed = items.removeIf(i -> i.getItemId().equals(itemId));
        if (!removed)
            throw new IllegalArgumentException("Item not found: " + itemId);
    }

    public OrderItem removeTicket(UUID itemId) {

        if (status != Status.ACTIVE)
            throw new IllegalStateException("Order is not active");

        if (isExpired())
            throw new IllegalStateException("Order has expired");

        for (OrderItem item : items) {
            if (item.getItemId().equals(itemId)) {
                items.remove(item);
                return item;
            }
        }
        throw new IllegalArgumentException("Item not found: " + itemId);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt)
                || status == Status.EXPIRED
                || status == Status.CANCELLED;
    }

    public boolean isOwnedBy(String buyerId) {
        return this.buyerId.equals(buyerId);
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : items) {
            total = total.add(item.getPrice());
        }

        return total;
    }

    // Apply discount (provided by PolicyService)
    public void applyDiscount(BigDecimal discount) {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("invalid discount");

        this.discount = discount;
    }

    /**
     * Final price — updated every time PolicyService is called.
     * If PolicyService was not called yet, returns original total.
     */
    public BigDecimal getFinalPrice() {
        return finalPrice != null ? finalPrice : getTotal();
    }

    /** Discount = total - finalPrice */
    public BigDecimal getDiscount() {
        return getTotal().subtract(getFinalPrice());
    }

    /**
     * Updates finalPrice directly from PolicyService result.
     * Called after every PolicyService call:
     * - applyGeneralDiscounts (type b) in reserveTickets / removeFromOrder
     * - calculateCouponDiscount (type c) in applyCoupon
     * If no discount applies, PolicyService returns original total → finalPrice =
     * total → discount = 0.
     */
    public void updateFinalPrice(double priceFromPolicyService) {
        BigDecimal price = BigDecimal.valueOf(priceFromPolicyService);
        if (price.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("finalPrice cannot be negative");
        if (price.compareTo(getTotal()) > 0)
            throw new IllegalArgumentException("finalPrice cannot exceed total");
        this.finalPrice = price;
        this.discount = getTotal().subtract(this.finalPrice);
    }

    /**
     * Clears all locks from items and returns their resource IDs.
     * Service uses the returned IDs to release locks in the repository.
     */
    public List<String> releaseAllLocks() {
        List<String> lockIds = new ArrayList<>();
        for (OrderItem item : items) {
            lockIds.add(item.getLockResourceId());
            item.clearLock();
        }
        return lockIds;
    }

    public void setAppliedCouponCode(String code) {
        this.appliedCouponCode = code;
    }

    public void markCompleted() {
        this.status = Status.COMPLETED;
    }

    public void markExpired() {
        this.status = Status.EXPIRED;
    }

    public void markCancelled() {
        this.status = Status.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public List<OrderItem> getItems() {
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

    public String getAppliedCouponCode() {
        return appliedCouponCode;
    }
}
