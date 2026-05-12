package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persisted record of a single user action against an ActiveOrder. The
 * application service writes one of these every time it mutates an order on
 * the user's behalf; the most recent entry can be popped and inverted by
 * {@code ActiveOrderService.undoLast}.
 *
 * Snapshot fields are nullable and their meaning is type-dependent — see
 * the static factory methods.
 */
@Entity
@Table(name = "order_action_log")
public class OrderActionLog {

    public enum ActionType {
        /** User added a ticket to the order; inverse is to remove it. */
        ADD_TICKET,
        /** User removed a ticket; inverse is to add it back at the same price. */
        REMOVE_TICKET,
        /** User applied a coupon; inverse is to restore the prior finalPrice. */
        APPLY_COUPON
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ticket_id")           private String ticketId;
    @Column(name = "seat_id")             private Long seatId;
    @Column(name = "area_id")             private UUID areaId;
    @Column(name = "price")               private BigDecimal price;
    @Column(name = "coupon_code")         private String couponCode;
    @Column(name = "prior_final_price")   private BigDecimal priorFinalPrice;

    protected OrderActionLog() {}

    /** Inverse: re-add the same ticket to the order. */
    public static OrderActionLog forRemovedTicket(UUID orderId, OrderItem item) {
        OrderActionLog log = new OrderActionLog();
        log.orderId   = orderId;
        log.type      = ActionType.REMOVE_TICKET;
        log.createdAt = LocalDateTime.now();
        log.ticketId  = item.getTicketId();
        log.seatId    = item.getSeatId();
        log.areaId    = item.getAreaId();
        log.price     = item.getPrice();
        return log;
    }

    /** Inverse: remove the just-added ticket from the order. */
    public static OrderActionLog forAddedTicket(UUID orderId, OrderItem item) {
        OrderActionLog log = new OrderActionLog();
        log.orderId   = orderId;
        log.type      = ActionType.ADD_TICKET;
        log.createdAt = LocalDateTime.now();
        log.ticketId  = item.getTicketId();
        log.seatId    = item.getSeatId();
        log.areaId    = item.getAreaId();
        log.price     = item.getPrice();
        return log;
    }

    /** Inverse: revert finalPrice to {@code priorFinalPrice} and clear coupon. */
    public static OrderActionLog forAppliedCoupon(UUID orderId, String couponCode, BigDecimal priorFinalPrice) {
        OrderActionLog log = new OrderActionLog();
        log.orderId         = orderId;
        log.type            = ActionType.APPLY_COUPON;
        log.createdAt       = LocalDateTime.now();
        log.couponCode      = couponCode;
        log.priorFinalPrice = priorFinalPrice;
        return log;
    }

    public Long getId()                   { return id; }
    public UUID getOrderId()              { return orderId; }
    public ActionType getType()           { return type; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public String getTicketId()           { return ticketId; }
    public Long getSeatId()               { return seatId; }
    public UUID getAreaId()               { return areaId; }
    public BigDecimal getPrice()          { return price; }
    public String getCouponCode()         { return couponCode; }
    public BigDecimal getPriorFinalPrice() { return priorFinalPrice; }
}
