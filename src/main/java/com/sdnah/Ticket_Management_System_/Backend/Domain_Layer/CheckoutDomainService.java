package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Ticketcode;


public class CheckoutDomainService {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutDomainService.class);

    private final IPaymentGateway paymentGateway;
    private final ITicketSupplierGateway ticketGateway;
    private final Ticket_Domain_Service ticketDomainService;

    public CheckoutDomainService(IPaymentGateway paymentGateway, ITicketSupplierGateway ticketGateway,
            Ticket_Domain_Service ticketDomainService) {
        if (paymentGateway == null)
            throw new IllegalArgumentException("paymentGateway required");
        if (ticketGateway == null)
            throw new IllegalArgumentException("ticketGateway required");
        if (ticketDomainService == null)
            throw new IllegalArgumentException("ticketDomainService required");
        this.paymentGateway = paymentGateway;
        this.ticketGateway = ticketGateway;
        this.ticketDomainService = ticketDomainService;
    }

    public CheckoutResult checkout(ActiveOrder order, PaymentDetails details) {
        // 1. charge payment
        PaymentTransaction tx = paymentGateway.charge(order.getId(), order.getFinalPrice(), details);
        if (!tx.isSuccessful()) {
            logger.error("Payment FAILED orderId={} ", order.getId());
            // UNDO — release all tickets back to inventory + cancel order
            ticketDomainService.releaseAllTickets(order.cancel());
            throw new IllegalStateException("Payment failed — order cancelled, tickets released");
        }
 
        List<Ticketcode> codes;
        try {
            // 2. issue tickets
            codes = ticketGateway.issueTickets(order.getId(), order.getItems());
        } catch (Exception e) {
            // UNDO — refund payment + release all tickets back to inventory + cancel order
            logger.error("Ticket issuance FAILED orderId={} — refunding and releasing tickets (all or nothing)", order.getId());
            paymentGateway.refund(tx.getTransactionId());
            ticketDomainService.releaseAllTickets(order.cancel());
            throw new IllegalStateException("Ticket issuance failed — payment refunded, tickets released");
        }
 
        // 3. mark tickets as SOLD in EVENT aggregate — domain handles findById internally
        for (String lockId : order.releaseAllLocks())
            ticketDomainService.markTicketAsSold(order, lockId);
 
        order.markCompleted();
 
        Purchase purchase = new Purchase(order);
        // 6. collect ticket codes
        List<String> ticketCodes = new ArrayList<>();
        for (Ticketcode tc : codes) ticketCodes.add(tc.getCode());
 
        logger.info("checkout SUCCESS orderId={} purchaseId={}", order.getId(), purchase.getPurchaseId());
        return new CheckoutResult(purchase, tx, ticketCodes);
    }

    // Result object — carries all checkout output back to Application Layer
    public static class CheckoutResult {
        private final Purchase purchase;
        private final PaymentTransaction transaction;
        private final List<String> ticketCodes;
 
        public CheckoutResult(Purchase purchase, PaymentTransaction transaction, List<String> ticketCodes) {
            this.purchase    = purchase;
            this.transaction = transaction;
            this.ticketCodes = ticketCodes;
        }
 
        public Purchase getPurchase()              { return purchase; }
        public PaymentTransaction getTransaction() { return transaction; }
        public List<String> getTicketCodes()       { return ticketCodes; }
    }

}