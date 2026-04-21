package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;

public class ExpiredOrderService {
    private static final Logger logger = LoggerFactory.getLogger(ExpiredOrderService.class);
    private final IOrderRepository orderRepository;

    public ExpiredOrderService(IOrderRepository orderRepository) {
        if (orderRepository == null)
            throw new IllegalArgumentException("orderRepository required");
        this.orderRepository = orderRepository;
    }

    public void releaseExpiredOrders() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
