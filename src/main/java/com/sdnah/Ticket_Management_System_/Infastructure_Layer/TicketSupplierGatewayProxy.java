package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;

public class TicketSupplierGatewayProxy implements ITicketSupplierGateway {

    @Override
    public List<Ticketcode> issueTickets(UUID purchaseId, List<OrderItem> items) {
        List<Ticketcode> result = new ArrayList<>();

        for (OrderItem item : items) {
            Ticketcode ticket = new Ticketcode(
                    "TICKET-" + item.getTicketId(),
                    "QR-" + purchaseId + "-" + item.getTicketId());
            result.add(ticket);
        }
        return result;
    }

}
