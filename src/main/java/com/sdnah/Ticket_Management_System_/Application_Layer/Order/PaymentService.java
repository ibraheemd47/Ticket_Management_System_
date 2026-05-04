package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;

@Service

public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final IPaymentGateway paymentGateway;
    private final PaymentTransactionRepository transactionRepo;

    public PaymentService(IPaymentGateway paymentGateway,
            PaymentTransactionRepository transactionRepo) {
        if (paymentGateway == null)
            throw new IllegalArgumentException("paymentGateway required");
        if (transactionRepo == null)
            throw new IllegalArgumentException("transactionRepo required");
        this.paymentGateway = paymentGateway;
        this.transactionRepo = transactionRepo;
    }

    public PaymentTransaction charge(UUID orderId, BigDecimal amount, PaymentDetails details) {
        logger.info("Charging order {} amount {}", orderId, amount);
        PaymentTransaction tx = paymentGateway.charge(orderId, amount, details);
        transactionRepo.save(tx);
        logger.info("Charge result for order {}: {}", orderId, tx.getStatus());
        return tx;
    }

    public PaymentTransaction refund(String transactionId) {
        logger.info("Refunding transaction {}", transactionId);
        PaymentTransaction tx = paymentGateway.refund(transactionId);
        transactionRepo.save(tx);
        logger.info("Refund result for transaction {}: {}", transactionId, tx.getStatus());
        return tx;
    }

}
