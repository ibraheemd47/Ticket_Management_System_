package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order;

import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Ticketcode;

public interface ITicketSupplierGateway {
    List<Ticketcode> issueTickets(UUID purchaseId, List<OrderItem> items);

}
