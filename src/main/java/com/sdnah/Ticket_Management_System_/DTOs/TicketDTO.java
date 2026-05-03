package com.sdnah.Ticket_Management_System_.DTOs;
import java.util.UUID;

public class TicketDTO {
    public UUID id;
    public long eventId;
    public long showId;
    public String seatNumber;
    public double price;
    public boolean isLocked;
    public String lockedByUserId;

    public TicketDTO() {}

    public TicketDTO(UUID id, long eventId, long showId, String seatNumber, double price) {
        this.id = id;
        this.eventId = eventId;
        this.showId = showId;
        this.seatNumber = seatNumber;
        this.price = price;
        this.isLocked = false;
        this.lockedByUserId = null;
    }

}
