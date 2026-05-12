package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentTransactionRepository;

@Service

public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentTransactionRepository transactionRepo;

    public PaymentService(PaymentTransactionRepository transactionRepo) {
        if (transactionRepo == null)
            throw new IllegalArgumentException("transactionRepo required");
        this.transactionRepo = transactionRepo;
    }

    public void saveTransaction(PaymentTransaction tx) {
        logger.info("Saving transaction {}", tx.getTransactionId());
        transactionRepo.save(tx);
    }

}
