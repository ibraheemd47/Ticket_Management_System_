package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderItemDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.SeatRequest;

public class ActiveOrderService {
    private static final Logger logger = LoggerFactory.getLogger(ActiveOrderService.class);
    private static final int TTL_MINUTES = 10;
    private final IOrderRepository orderRepository;
    private final PaymentService paymentService;
    private final ITicketSupplierGateway ticketGateway;

    public ActiveOrderService(IOrderRepository orderRepository,
            PaymentService paymentService,
            ITicketSupplierGateway ticketGateway) {
        if (orderRepository == null)
            throw new IllegalArgumentException("orderRepository required");
        if (paymentService == null)
            throw new IllegalArgumentException("paymentService required");
        if (ticketGateway == null)
            throw new IllegalArgumentException("ticketGateway required");
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.ticketGateway = ticketGateway;
    }

    public OrderDTO reserveTickets(String buyerId, int eventId, List<SeatRequest> seats) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public OrderDTO removeFromOrder(UUID orderId, UUID itemId, String buyerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public PurchaseDTO checkout(UUID orderId, String buyerId, PaymentDetailsDTO paymentDTO) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public OrderDTO getActiveOrder(String buyerId, int eventId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<PurchaseDTO> getPurchaseHistory(String buyerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private ActiveOrder findValidOrder(UUID orderId, String buyerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
