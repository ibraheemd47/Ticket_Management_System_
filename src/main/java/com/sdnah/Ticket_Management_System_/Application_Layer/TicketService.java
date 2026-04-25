package com.sdnah.Ticket_Management_System_.Application_Layer;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    /**
     * When a user selects a ticket in React, we lock it so others can't buy it.
     */
    public boolean lockTicketForUser(UUID ticketId, String userId) {
        return ticketRepository.findById(ticketId)
            .map(ticket -> {
                boolean success = ticket.lockInCart(userId);
                if (success) ticketRepository.save(ticket);
                return success;
            })
            .orElse(false);
    }

    /**
     * Finalizing the purchase after payment is successful.
     */
    public boolean confirmPurchase(UUID ticketId, String userId) {
        return ticketRepository.findById(ticketId)
            .map(ticket -> {
                boolean success = ticket.purchase(userId);
                if (success) {
                    // Logic for generating QR code or sending email could go here
                    ticketRepository.save(ticket);
                }
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
    }

    /**
     * Used at the venue entrance (Scanning the QR code).
     */
    public boolean scanTicketAtDoor(UUID ticketId) {
        return ticketRepository.findById(ticketId)
            .map(ticket -> {
                boolean success = ticket.scan();
                if (success) ticketRepository.save(ticket);
                return success;
            })
            .orElse(false);
    }

    /**
     * Fetching all tickets owned by a specific user (for their Profile page).
     */
    public List<ticket> getTicketsByOwner(UUID ownerId) {
        return ticketRepository.findByOwnerId(ownerId);
    }
}