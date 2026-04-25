package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;
<<<<<<< HEAD
=======


import java.math.BigDecimal;
import java.util.UUID;

>>>>>>> main
public class ticket {
    
    private final String ticketId; // QR code
    private final Long showId;     // The show this ticket is for
    private String ownerId;        // The user who bought it

    // The Ticket holds a reference to the exact Seat object
    private final Seat seat; 
    
    private BigDecimal price;
    private TicketStatus status;

    public enum TicketStatus {
        AVAILABLE,
        LOCKED_IN_CART, // Someone is currently checking out
        PURCHASED,
        SCANNED         // Used at the door
    }

    // Constructor
    public ticket(Long showId, Seat seat, BigDecimal price) {
        this.ticketId = null ; // we want to generate a QR code Based on the ticketId.
        this.showId = showId;
        this.seat = seat;
        this.price = price;
        this.status = TicketStatus.AVAILABLE;
    }

    // Domain Logic: Buying the ticket
    public boolean purchase(String userId) {
        if (this.status != TicketStatus.AVAILABLE) {
            return false; 
        }
        this.ownerId = userId;
        this.status = TicketStatus.PURCHASED;
        return true;
    }

}
