package com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public class PurchaseDTO {
    private final UUID purchaseId;
    private final UUID orderId;
    private final List<String> ticketCodes;
    private final BigDecimal finalPrice;
    private final LocalDateTime purchasedAt;

    public PurchaseDTO(UUID purchaseId,
            UUID orderId,
            List<String> ticketCodes,
            BigDecimal finalPrice,
            LocalDateTime purchasedAt) {

        this.purchaseId = purchaseId;
        this.orderId = orderId;
        this.ticketCodes = ticketCodes;
        this.finalPrice = finalPrice;
        this.purchasedAt = purchasedAt;
    }

    public UUID getPurchaseId() {
        return purchaseId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public List<String> getTicketCodes() {
        return ticketCodes;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

}
