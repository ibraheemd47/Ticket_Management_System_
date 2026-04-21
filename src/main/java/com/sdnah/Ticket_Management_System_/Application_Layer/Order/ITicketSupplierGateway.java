package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;

public interface ITicketSupplierGateway {
    List<Ticketcode> issueTickets(UUID purchaseId, List<OrderItem> items);

    void cancelTickets(UUID purchaseId);

}
