package com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs;

import java.math.BigDecimal;
import java.util.UUID;

public class SeatRequest {
    private final String ticketId;
    private final Long seatId;
    private final UUID areaId;
    private final BigDecimal price;

    public SeatRequest(String ticketId, Long seatId, UUID areaId, BigDecimal price) {
        this.ticketId = ticketId;
        this.seatId = seatId;
        this.areaId = areaId;
        this.price = price;
    }

    public String getTicketId() {
        return ticketId;
    }

    public long getSeatId() {
        return seatId;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public BigDecimal getPrice() {
        return price;
    }

}
