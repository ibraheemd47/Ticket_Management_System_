package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.CompensableStep;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderSaga;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Ticketcode;

public class CheckoutDomainService {
    private static final Logger log = LoggerFactory.getLogger(Checkout_Domain_Service.class);

    private final IPaymentGateway paymentGateway;
    private final ITicketSupplierGateway ticketGateway;

    public CheckoutDomainService(IPaymentGateway paymentGateway, ITicketSupplierGateway ticketGateway) {
        if (paymentGateway == null)
            throw new IllegalArgumentException("paymentGateway required");
        if (ticketGateway == null)
            throw new IllegalArgumentException("ticketGateway required");
        this.paymentGateway = paymentGateway;
        this.ticketGateway = ticketGateway;
    }

    public CheckoutResult checkout(ActiveOrder order, PaymentDetails details) {
        if (order == null)
            throw new IllegalArgumentException("order required");
        if (details == null)
            throw new IllegalArgumentException("details required");

        OrderSaga saga = new OrderSaga();
        AtomicReference<PaymentTransaction> txRef = new AtomicReference<>();
        AtomicReference<List<Ticketcode>> codesRef = new AtomicReference<>();

        try {
            saga.run(chargePaymentStep(order, details, txRef));
            saga.run(issueTicketsStep(order, codesRef));
            saga.run(finalizeOrderStep(order));
        } catch (RuntimeException ex) {
            // Saga has already compensated; translate to a domain failure.
            throw new IllegalStateException(
                    "Checkout failed for order " + order.getId() + ": " + ex.getMessage(), ex);
        }

        Purchase purchase = new Purchase(order);
        List<String> ticketCodes = new ArrayList<>();
        for (Ticketcode tc : codesRef.get())
            ticketCodes.add(tc.getCode());

        log.info("checkout SUCCESS orderId={} purchaseId={}", order.getId(), purchase.getPurchaseId());
        return new CheckoutResult(purchase, txRef.get(), ticketCodes);
    }

    // ── Steps ────────────────────────────────────────────────────────────────

    private CompensableStep chargePaymentStep(ActiveOrder order,
            PaymentDetails details,
            AtomicReference<PaymentTransaction> txRef) {
        return new CompensableStep() {
            @Override
            public void execute() {
                PaymentTransaction tx = paymentGateway.charge(order.getId(), order.getFinalPrice(), details);
                if (tx == null || !tx.isSuccessful()) {
                    throw new IllegalStateException("payment declined for order " + order.getId());
                }
                txRef.set(tx);
            }

            @Override
            public void compensate() {
                PaymentTransaction tx = txRef.get();
                if (tx == null)
                    return; // nothing charged
                if (tx.isRefunded())
                    return; // idempotent
                paymentGateway.refund(tx.getTransactionId());
                log.info("refunded transaction {} for order {}", tx.getTransactionId(), order.getId());
            }

            @Override
            public String name() {
                return "charge-payment";
            }
        };
    }

    private CompensableStep issueTicketsStep(ActiveOrder order,
            AtomicReference<List<Ticketcode>> codesRef) {
        return new CompensableStep() {
            @Override
            public void execute() {
                List<Ticketcode> codes = ticketGateway.issueTickets(order.getId(), order.getItems());
                if (codes == null || codes.isEmpty()) {
                    throw new IllegalStateException("ticket supplier returned no codes for order " + order.getId());
                }
                codesRef.set(codes);
            }

            @Override
            public void compensate() {
                List<Ticketcode> codes = codesRef.get();
                if (codes == null || codes.isEmpty())
                    return;
                // The supplier gateway has no void operation. Log loudly so an
                // operator can reconcile; downstream alerting hooks should pick
                // this up from the log marker below.
                log.error("ORPHANED_TICKET_CODES orderId={} codes={} — manual void required",
                        order.getId(), codes.size());
            }

            @Override
            public String name() {
                return "issue-tickets";
            }
        };
    }

    private CompensableStep finalizeOrderStep(ActiveOrder order) {
        return new CompensableStep() {
            @Override
            public void execute() {
                order.releaseAllLocks();
                order.markCompleted();
            }

            @Override
            public void compensate() {
                // releaseAllLocks already cleared the lock fields; re-cancel
                // so the order is not mistakenly reachable as COMPLETED.
                order.markCancelled();
            }

            @Override
            public String name() {
                return "finalize-order";
            }
        };
    }

    // ── Result ───────────────────────────────────────────────────────────────

    public static final class CheckoutResult {
        private final Purchase purchase;
        private final PaymentTransaction transaction;
        private final List<String> ticketCodes;

        public CheckoutResult(Purchase purchase, PaymentTransaction transaction, List<String> ticketCodes) {
            this.purchase = purchase;
            this.transaction = transaction;
            this.ticketCodes = ticketCodes;
        }

        public Purchase getPurchase() {
            return purchase;
        }

        public PaymentTransaction getTransaction() {
            return transaction;
        }

        public List<String> getTicketCodes() {
            return ticketCodes;
        }
    }
}