package com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemDTO {
    private final UUID itemId;
    private final String ticketId;
    private final Long seatId;
    private final UUID areaId;
    private final BigDecimal price;

    public OrderItemDTO(UUID itemId, String ticketId, Long seatId, UUID areaId, BigDecimal price) {
        this.itemId = itemId;
        this.ticketId = ticketId;
        this.seatId = seatId;
        this.areaId = areaId;
        this.price = price;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public BigDecimal getPrice() {
        return price;
    }

}
