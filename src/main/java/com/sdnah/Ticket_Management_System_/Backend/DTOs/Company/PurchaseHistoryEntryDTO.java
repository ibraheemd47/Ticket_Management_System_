package com.sdnah.Ticket_Management_System_.Backend.DTOs.Company;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row of a company's purchase / order history (II.4.5). Each entry
 * corresponds to a single completed {@code Purchase} and carries enough
 * data for the Vaadin grid to render without loading the underlying
 * entities again.
 */
public class PurchaseHistoryEntryDTO {

    private final UUID purchaseId;
    private final UUID orderId;
    private final UUID eventId;
    private final String eventName;
    private final String buyerId;
    private final int ticketCount;
    private final BigDecimal totalPrice;
    private final LocalDateTime purchasedAt;

    public PurchaseHistoryEntryDTO(UUID purchaseId,
                                   UUID orderId,
                                   UUID eventId,
                                   String eventName,
                                   String buyerId,
                                   int ticketCount,
                                   BigDecimal totalPrice,
                                   LocalDateTime purchasedAt) {
        this.purchaseId = purchaseId;
        this.orderId = orderId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.buyerId = buyerId;
        this.ticketCount = ticketCount;
        this.totalPrice = totalPrice == null ? BigDecimal.ZERO : totalPrice;
        this.purchasedAt = purchasedAt;
    }

    public UUID getPurchaseId()         { return purchaseId; }
    public UUID getOrderId()            { return orderId; }
    public UUID getEventId()            { return eventId; }
    public String getEventName()        { return eventName; }
    public String getBuyerId()          { return buyerId; }
    public int getTicketCount()         { return ticketCount; }
    public BigDecimal getTotalPrice()   { return totalPrice; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
}
