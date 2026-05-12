package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order;

import java.math.BigDecimal;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;

public interface IPaymentGateway {
    PaymentTransaction charge(UUID orderId, BigDecimal amount, PaymentDetails details);

    PaymentTransaction refund(String transactionId);
}
