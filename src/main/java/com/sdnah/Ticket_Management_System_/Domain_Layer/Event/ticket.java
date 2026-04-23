package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;


import java.math.BigDecimal;
import java.util.Date;

public class ticket {
    
    private final String ticketId; // QR code
    private final Long showId;     // The show this ticket is for
    private String ownerId;        // The user who bought it

    // The Ticket holds a reference to the exact Seat object
    private final Seat seat; 
    private final Area area; // The area this ticket belongs to
    private final Row row;   // The row this ticket belongs to
    private final Block block; // The block this ticket belongs to
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
    public ticket(Long showId, Seat seat, Area area, Row row, Block block, Date showDate, BigDecimal price) {
        this.ticketId = null ; // we want to generate a QR code Based on the ticketId.
        this.showId = showId;
        this.seat = seat;
        this.area = area;
        this.row = row;
        this.block = block;
        this.showDate = showDate;
        this.price = price;
        this.status = TicketStatus.AVAILABLE;
    }
    //ticket for stranding areas
    public ticket(Long showId, Area area, Date showDate, BigDecimal price) {
        this.ticketId = null ; // we want to generate a QR code Based on the ticketId.
        this.showId = showId;
        this.seat = null; // No specific seat for standing areas
        this.area = area;
        this.row = null; // No specific row for standing areas
        this.block = null; // No specific block for standing areas
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
    public String getTicketId() {
        return ticketId;
    }

}
