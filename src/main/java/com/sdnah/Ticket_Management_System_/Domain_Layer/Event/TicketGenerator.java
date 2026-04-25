package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TicketGenerator {

    // This method takes a Show and loops through its physical layout to map tickets
    public List<ticket> generateAllTicketsForShow(show show, BigDecimal defaultPrice) {
        
        List<ticket> generatedTickets = new ArrayList<>();

        // Loop through all areas in the show
        for (Area area : show.getAreas()) {
            
            // Handle Standing Areas
            if (area instanceof StandingArea) {
                StandingArea standingArea = (StandingArea) area;
                // Generate a ticket for every person up to the max capacity
                for (int i = 0; i < standingArea.getMaxCapacity(); i++) {
                    ticket ticket = new ticket(UUID.randomUUID(), show.getShowid(), standingArea, show.getShowDate(), defaultPrice);
                    generatedTickets.add(ticket);
                }
            } 
            // Handle Seated Areas
            else if (area instanceof SeatedArea) {
                SeatedArea seatedArea = (SeatedArea) area;
                
                // Deep nested loops to drill down to the individual seats
                for (Block block : seatedArea.getBlocks()) {
                    for (Row row : block.getRows()) {
                        for (Seat seat : row.getSeats()) {
                            
                            // You can even put custom pricing logic here! 
                            // e.g., if (block.getName().equals("VIP")) { price = 500; }
                            
                            ticket ticket = new ticket(UUID.randomUUID(), show.getShowid(), seat, seatedArea, show.getShowDate(), defaultPrice);
                            generatedTickets.add(ticket);
                        }
                    }
                }
            }
        }

        return generatedTickets;
    }
}