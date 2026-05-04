package com.sdnah.Ticket_Management_System_.Domain_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;

public class Ticket_Domain_Service {


    public Ticket_Domain_Service() {
     
    }
    public void TicketSold(ActiveOrder order, ticket ticket) {
        // Mark the ticket as sold
        ticket.purchase(order.getId());
        // Additional logic like sending confirmation email, updating inventory, etc.
    
    }
    public void TicketCancelled(ActiveOrder order, ticket ticket) {
        // Mark the ticket as available again
        ticket.cancel();
        // Additional logic like sending cancellation email, updating inventory, etc.
    }


}
