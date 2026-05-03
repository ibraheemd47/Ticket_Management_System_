package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;

@Service

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
        logger.info("Charging order {} amount {}", orderId, amount);
        PaymentTransaction tx = paymentGateway.charge(orderId, amount, details);
        orderRepository.saveTransaction(tx);
        logger.info("Charge result for order {}: {}", orderId, tx.getStatus());
        return tx;
    }

    public PaymentTransaction refund(String transactionId) {
        logger.info("Refunding transaction {}", transactionId);
        PaymentTransaction tx = paymentGateway.refund(transactionId);
        orderRepository.saveTransaction(tx);
        logger.info("Refund result for transaction {}: {}", transactionId, tx.getStatus());
        return tx;
    }

}
