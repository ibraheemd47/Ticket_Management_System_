package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;

import ch.qos.logback.classic.Logger;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;
    private final Logger logger = (Logger) LoggerFactory.getLogger(TicketService.class);

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * When a user selects a ticket in React, we lock it so others can't buy it.
     */
    public boolean lockTicketForUser(UUID ticketId, UUID userId) {
        return ticketRepository.findById(ticketId)
                .map(ticket -> {
                    boolean success = ticket.lockInCart(userId);
                    if (success)
                        ticketRepository.save(ticket);
                    logger.info("Ticket {} locked in cart for user {}", ticketId, userId);
                    return success;
                })
                .orElse(false);
    }

    /**
     * Finalizing the purchase after payment is successful.
     */
    public boolean confirmPurchase(UUID ticketId, UUID userId) {
        return ticketRepository.findById(ticketId)
                .map(ticket -> {
                    boolean success = ticket.purchase(userId);
                    if (success) {
                        // Logic for generating QR code or sending email could go here
                        ticketRepository.save(ticket);
                    }
                    logger.info("Ticket {} confirmed for user {}", ticketId, userId);
                    return success;
                })
                .orElse(false);
    }

    /**
     * If the user removes the item from cart or the timer expires.
     */
    public void releaseTicket(UUID ticketId) {
        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            ticket.unlockFromCart();
            ticketRepository.save(ticket);
        });
        logger.info("Ticket {} released from cart", ticketId);
    }

    /**
     * Used at the venue entrance (Scanning the QR code).
     */
    public boolean scanTicketAtDoor(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .map(ticket -> {
                    boolean success = ticket.scan();
                    if (success)
                        ticketRepository.save(ticket);
                    logger.info("Ticket {} scanned at door", ticketId);
                    return success;
                })
                .orElse(false);
    }

    /**
     * Returns a map of seatId → true/false (true = at least one AVAILABLE ticket
     * exists for that seat).
     * Used by the seat-selection view to colour seats green/red.
     */
    public Map<Long, Boolean> getSeatAvailability(UUID showId) {
        return ticketRepository.findByShowId(showId).stream()
                .filter(t -> t.getSeat() != null)
                .collect(Collectors.toMap(
                        t -> t.getSeat().getId(),
                        t -> t.getStatus() == ticket.TicketStatus.AVAILABLE,
                        (a, b) -> a || b));
    }

    /**
     * Fetching all tickets owned by a specific user (for their Profile page).
     */
    public List<ticket> getTicketsByOwner(UUID ownerId) {
        logger.info("Fetching tickets for owner {}", ownerId);

        return ticketRepository.findByOwnerId(ownerId);
    }

    public int getStandingUsedCount(UUID showId, UUID areaId) {
        return (int) ticketRepository.countByShowIdAndArea_IdAndSeatIsNullAndStatusIn(
                showId,
                areaId,
                Set.of(
                        ticket.TicketStatus.LOCKED_IN_CART,
                        ticket.TicketStatus.PURCHASED,
                        ticket.TicketStatus.SCANNED));
    }

    public int getAreaUsedCount(UUID showId, UUID areaId) {
        return (int) ticketRepository.countByShowIdAndArea_IdAndStatusIn(
                showId,
                areaId,
                Set.of(
                        ticket.TicketStatus.LOCKED_IN_CART,
                        ticket.TicketStatus.PURCHASED,
                        ticket.TicketStatus.SCANNED));
    }

    public int getStandingAvailableCount(UUID showId, UUID areaId, int maxCapacity) {
        int used = getStandingUsedCount(showId, areaId);
        return Math.max(0, maxCapacity - used);
    }
}