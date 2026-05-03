package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@Entity
public class ticket {

    @Transient
    private Logger logger = (Logger) LoggerFactory.getLogger(ticket.class);

    @Id
    private UUID ticketId;

    private UUID showId;
    private String ownerId;

    @ManyToOne
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @ManyToOne
    @JoinColumn(name = "area_id")
    private Area area;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    private Date showDate; 

    public enum TicketStatus {
        AVAILABLE,
        LOCKED_IN_CART,
        PURCHASED,
        SCANNED
    }

    protected ticket() {}

    public ticket(UUID ticketId, UUID showId, Seat seat, Area area, Date showDate, BigDecimal price) {
        this.ticketId = ticketId;
        this.showId = showId;
        this.seat = seat;
        this.area = area;
        this.showDate = showDate;
        this.price = price;
        this.status = TicketStatus.AVAILABLE;
        this.ownerId = null;
        logger.info("Ticket created with ID: {} for show {}", this.ticketId, this.showId);
    }

    public ticket(UUID ticketId, UUID showId, Area standingArea, Date showDate, BigDecimal price) {
        this.ticketId = ticketId;
        this.showId = showId;
        this.area = standingArea;
        this.seat = null;
        this.showDate = showDate;
        this.price = price;
        this.status = TicketStatus.AVAILABLE;
        logger.info("Ticket created with ID: {} for standing area {}", this.ticketId, this.area.getId());
    }

    public boolean purchase(String userId) {
        if (this.status != TicketStatus.LOCKED_IN_CART) {
            logger.warn("Attempted to purchase ticket with ID: {} that is not locked in cart", this.ticketId);
            return false;
        }
        this.ownerId = userId;
        this.status = TicketStatus.PURCHASED;
        logger.info("Ticket purchased with ID: {} by user {}", this.ticketId, this.ownerId);
        return true;
    }

    public boolean lockInCart(String userId) {
        if (this.status != TicketStatus.AVAILABLE) {
            logger.warn("Attempted to lock ticket with ID: {} that is not available", this.ticketId);
            return false;
        }
        this.status = TicketStatus.LOCKED_IN_CART;
        this.ownerId = userId;
        logger.info("Ticket locked in cart with ID: {} for user {}", this.ticketId, this.ownerId);
        return true;
    }

    public boolean unlockFromCart() {
        if (this.status != TicketStatus.LOCKED_IN_CART) {
            logger.warn("Attempted to unlock ticket with ID: {} that is not locked in cart", this.ticketId);
            return false;
        }
        this.ownerId = null;
        this.status = TicketStatus.AVAILABLE;
        logger.info("Ticket unlocked from cart with ID: {}", this.ticketId);
        return true;
    }

    public boolean scan() {
        if (this.status != TicketStatus.PURCHASED) {
            logger.warn("Attempted to scan ticket with ID: {} that is not purchased", this.ticketId);
            return false;
        }
        this.status = TicketStatus.SCANNED;
        logger.info("Ticket scanned with ID: {}", this.ticketId);
        return true;
    }

    public boolean cancel() {
        if (this.status != TicketStatus.PURCHASED) {
            logger.warn("Attempted to cancel ticket with ID: {} that is not purchased", this.ticketId);
            return false;
        }
        this.ownerId = null;
        this.status = TicketStatus.AVAILABLE;
        logger.info("Ticket canceled with ID: {}", this.ticketId);
        return true;
    }

    public UUID getTicketId() { return ticketId; }
    public UUID getShowId() { return showId; }
    public String getOwnerId() { return ownerId; }
    public Seat getSeat() { return seat; }
    public Area getArea() { return area; }
    public BigDecimal getPrice() { return price; }
    public TicketStatus getStatus() { return status; }
    public Date getShowDate() { return showDate; }

    public String getFullSeatLocation() {
        if (this.seat == null) {
            logger.info("Ticket {} is for a standing area, no specific seat assigned", this.ticketId);
            return "General Admission - " + this.area.getName();
        }
        String seatNum = this.seat.getSeatNumber();
        String rowNum = this.seat.getRow().getRowNumber();
        String blockName = this.seat.getRow().getBlock().getBlockIdentifier();
        logger.info("Retrieving full seat location for ticket {}: Block {}, Row {}, Seat {}",
                this.ticketId, blockName, rowNum, seatNum);
        return blockName + ", " + rowNum + ", " + seatNum;
    }
}
