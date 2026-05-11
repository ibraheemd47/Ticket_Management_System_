package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer;

import java.util.UUID;

import org.slf4j.LoggerFactory;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;

import ch.qos.logback.classic.Logger;

public class Ticket_Domain_Service {

    private Logger logger = (Logger) LoggerFactory.getLogger(Ticket_Domain_Service.class);

    public Ticket_Domain_Service() {

    }

    public void TicketSold(ActiveOrder order, ticket ticket) {
        // Mark the ticket as sold
        logger.info("Marking ticket {} as sold for order {}", ticket.getTicketId(), order.getId());
        ticket.purchase(order.getId());
        // Additional logic like sending confirmation email, updating inventory, etc.

    }

    public void TicketLocked(String buyerId, ticket ticket) {
        // Lock the ticket to prevent it from being sold to someone else
        logger.info("Locking ticket {} for buyer {}", ticket.getTicketId(), buyerId);
        UUID buyerUUID = UUID.fromString(buyerId);
        ticket.lockInCart(buyerUUID);
        // Additional logic like setting a timer to unlock the ticket after a certain
        // period, etc.

    }

    public void TicketAvailable(ticket t) {
        // Unlock the ticket to make it available for others
        logger.info("Unlocking ticket {} and making it available again", t.getTicketId());
        t.unlockFromCart();
    }

}
