package com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemDTO {
    private final UUID itemId;
    private final String ticketId;
    private final int seatId;
    private final int areaId;
    private final BigDecimal price;

    public OrderItemDTO(UUID itemId, String ticketId, int seatId, int areaId, BigDecimal price) {
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

    public int getSeatId() {
        return seatId;
    }

    public int getAreaId() {
        return areaId;
    }

    public BigDecimal getPrice() {
        return price;
    }

}
