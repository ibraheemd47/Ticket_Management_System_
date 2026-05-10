package com.sdnah.Ticket_Management_System_.Domain_Layer;

import java.util.UUID;

import org.slf4j.LoggerFactory;
import java.util.List;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;

import ch.qos.logback.classic.Logger;

public class Ticket_Domain_Service {

    private Logger logger = (Logger) LoggerFactory.getLogger(Ticket_Domain_Service.class);
    private final TicketRepository ticketRepository;

    public Ticket_Domain_Service(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public void markTicketAsSold(ActiveOrder order, String ticketId) {
        logger.info("Ticket {} marked as SOLD for order {}", ticketId, order.getId());

        ticket ticket = ticketRepository.findById(UUID.fromString(ticketId))
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        ticket.purchase(order.getId());
    }

    public void releaseAllTickets(List<String> ticketIds) {
        for (String ticketId : ticketIds) {
            logger.info("Unlocking ticket {} and making it available again", ticketId);
            ticketRepository.findById(UUID.fromString(ticketId))
                    .ifPresent(t -> t.unlockFromCart());
        }
    }

    public void lockAllTickets(ActiveOrder order, List<String> ticketIds) {
        for (String ticketId : ticketIds) {
            logger.info("Locking ticket {} for order {}", ticketId, order.getId());
            ticketRepository.findById(UUID.fromString(ticketId))
                    .ifPresent(t -> t.lockInCart(UUID.fromString(order.getbuyerId())));
        }
    }

    public void TicketLocked(String buyerId, ticket ticket) {
        // Lock the ticket to prevent it from being sold to someone else
        logger.info("Locking ticket {} for buyer {}", ticket.getTicketId(), buyerId);
        UUID buyerUUID = UUID.fromString(buyerId);
        ticket.lockInCart(buyerUUID);
        // Additional logic like setting a timer to unlock the ticket after a certain
        // period, etc.

    }

    

}
