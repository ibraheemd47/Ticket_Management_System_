package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;

public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final IPaymentGateway paymentGateway;
    private final IOrderRepository orderRepository;

    public PaymentService(IPaymentGateway paymentGateway, IOrderRepository orderRepository) {
        if (paymentGateway == null)
            throw new IllegalArgumentException("paymentGateway required");
        if (orderRepository == null)
            throw new IllegalArgumentException("orderRepository required");
        this.paymentGateway = paymentGateway;
        this.orderRepository = orderRepository;
    }

    public PaymentTransaction charge(UUID orderId, BigDecimal amount, PaymentDetails details) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public PaymentTransaction refund(String transactionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
