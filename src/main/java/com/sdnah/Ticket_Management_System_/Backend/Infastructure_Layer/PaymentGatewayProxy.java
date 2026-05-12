package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;

@Component
public class PaymentGatewayProxy implements IPaymentGateway {
    @Override
    public PaymentTransaction charge(UUID orderId, BigDecimal amount, PaymentDetails details) {
        return new PaymentTransaction(UUID.randomUUID().toString(), orderId, amount,
                PaymentTransaction.Status.SUCCESS);
    }

    @Override
    public PaymentTransaction refund(String transactionId) {
        return new PaymentTransaction(transactionId, UUID.randomUUID(), BigDecimal.ZERO,
                PaymentTransaction.Status.REFUNDED);
    }

}
