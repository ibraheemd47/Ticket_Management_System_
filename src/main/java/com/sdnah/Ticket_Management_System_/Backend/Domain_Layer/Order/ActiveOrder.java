package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "active_orders")
public class ActiveOrder {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "buyer_id")
    private String buyerId;

    @Column(name = "event_id")
    private UUID eventId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Version
    @Column(name = "version")
    private long version;

    @Column(name = "discount")
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "applied_coupon_code")
    private String appliedCouponCode;

    @Column(name = "final_price")
    private BigDecimal finalPrice = null;

    public enum Status {
        ACTIVE, EXPIRED, COMPLETED, CANCELLED
    }

    // JPA required
    protected ActiveOrder() {
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

    public String getbuyerId() {
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
