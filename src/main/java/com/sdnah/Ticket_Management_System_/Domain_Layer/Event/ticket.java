package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;


import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class ticket {
    
    private final UUID ticketId; // QR code
    private final UUID showId;     // The show this ticket is for
    private String ownerId;        // The user who bought it

    // The Ticket holds a reference to the exact Seat object
    private final Seat seat; 
    private final Area area; // The area this ticket belongs to
    private BigDecimal price;
    private TicketStatus status;
    private Date showDate; // The date of the show, useful for sorting and filtering

    public enum TicketStatus {
        AVAILABLE,
        LOCKED_IN_CART, // Someone is currently checking out
        PURCHASED,
        SCANNED         // Used at the door
    }
    
    // Constructor
    public ticket(UUID ticketId, UUID showId, Seat seat, Area area,  Date showDate, BigDecimal price) {
        this.ticketId = ticketId;
        this.showId = showId;
        this.seat = seat;
        this.area = area;
        this.showDate = showDate;
        this.price = price;
        this.status = TicketStatus.AVAILABLE;
    }
    //ticket for stranding areas
    public ticket(UUID ticketId, UUID showId, Area standingArea, Date showDate, BigDecimal price) {
        this.ticketId = ticketId;
        this.showId = showId;
        this.area = standingArea;
        this.seat = null; // No specific seat for standing area
        this.showDate = showDate;
        this.price = price;
        this.status = TicketStatus.AVAILABLE;
    }

    // Domain Logic: Buying the ticket
    public boolean purchase(String userId) {
        if (this.status != TicketStatus.LOCKED_IN_CART) {
            return false; 
        }
        this.ownerId = userId;
        this.status = TicketStatus.PURCHASED;
        return true;
    }
    public boolean lockInCart(String userId) {
        if (this.status != TicketStatus.AVAILABLE) {
            return false; 
        }
        this.status = TicketStatus.LOCKED_IN_CART;
        this.ownerId = userId;
        return true;
    }
    public boolean unlockFromCart() {
        if (this.status != TicketStatus.LOCKED_IN_CART) {
            return false; 
        }
        this.ownerId = null;
        this.status = TicketStatus.AVAILABLE;
        return true;
    }
    public boolean scan() {
        if (this.status != TicketStatus.PURCHASED) {
            return false; 
        }
        this.status = TicketStatus.SCANNED;
        return true;
    }
    public boolean cancel() {
        if (this.status != TicketStatus.PURCHASED) {
            return false; 
        }
        this.ownerId = null;
        this.status = TicketStatus.AVAILABLE;
        return true;
    }
    public UUID getTicketId() {
        return ticketId;
    }


    public String getFullSeatLocation() {
    if (this.seat == null) {
        return "General Admission - " + this.area.getName();
    }
    
    // Walking up the hierarchy!
    String seatNum = this.seat.getSeatNumber();
    String rowNum = this.seat.getRow().getRowNumber();
    String blockName = this.seat.getRow().getBlock().getBlockIdentifier();
    
    return blockName + ", " + rowNum + ", " + seatNum;
}

}
