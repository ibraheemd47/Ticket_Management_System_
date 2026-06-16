package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;

/**
 * Adapter that fulfils {@link IPaymentGateway} by delegating to {@link WsepClient}.
 * Translates our domain types ({@link PaymentDetails}, {@link PaymentTransaction})
 * into the form parameters WSEP expects, and the integer response back into a
 * {@link PaymentTransaction}.
 *
 * <p>NOT a {@code @Component} yet — Phase 2 will wire this in via Spring config
 * with a {@code @ConditionalOnProperty("wsep.enabled")} switch so the existing
 * in-memory stubs keep winning while V3 work is in progress.
 */
public class WsepPaymentGateway implements IPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(WsepPaymentGateway.class);

    /** Fallback currency until {@link PaymentDetails} carries one explicitly. */
    private static final String DEFAULT_CURRENCY = "USD";

    private final WsepClient client;

    public WsepPaymentGateway(WsepClient client) {
        this.client = client;
    }

    @Override
    public PaymentTransaction charge(UUID orderId, BigDecimal amount, PaymentDetails details) {
        if (orderId == null)  throw new IllegalArgumentException("orderId required");
        if (amount == null)   throw new IllegalArgumentException("amount required");
        if (details == null)  throw new IllegalArgumentException("details required");
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be > 0");

        WsepPaymentRequest req = toRequest(amount, details);
        int wsepTxnId = client.pay(req);

        if (wsepTxnId == WsepClient.FAILURE) {
            log.warn("WSEP declined payment for orderId={}", orderId);
            // PaymentTransaction requires a non-blank transactionId even on
            // failure, so we mint a synthetic one and mark it FAILED.
            return new PaymentTransaction(
                    "wsep-fail-" + UUID.randomUUID(), orderId, amount,
                    PaymentTransaction.Status.FAILED);
        }

        log.info("WSEP charged orderId={} wsepTxnId={}", orderId, wsepTxnId);
        return new PaymentTransaction(
                String.valueOf(wsepTxnId), orderId, amount,
                PaymentTransaction.Status.SUCCESS);
    }

    @Override
    public PaymentTransaction refund(String transactionId) {
        if (transactionId == null || transactionId.isBlank())
            throw new IllegalArgumentException("transactionId required");

        int rc = client.refund(transactionId);
        PaymentTransaction.Status status = (rc == 1)
                ? PaymentTransaction.Status.REFUNDED
                : PaymentTransaction.Status.FAILED;

        if (rc != 1) {
            log.error("WSEP refused refund for transactionId={}", transactionId);
        }
        // WSEP refund doesn't echo the original orderId/amount back. The
        // saga's compensation step already holds the original transaction;
        // it only inspects status / isRefunded(), so a minimal record is
        // sufficient here.
        return new PaymentTransaction(transactionId, UUID.randomUUID(),
                BigDecimal.ZERO, status);
    }

    // ── mapping helpers ─────────────────────────────────────────────────────

    private static WsepPaymentRequest toRequest(BigDecimal amount, PaymentDetails details) {
        // PaymentDetails today carries cardToken + billingName + paymentMethod.
        // WSEP wants card_number / month / year / holder / cvv / id. We have
        // no card_number/month/year/cvv in the domain object, so for Phase 1
        // we derive what we can and use placeholders for the rest. A later
        // refinement (out of Phase 1 scope) is to extend PaymentDetails with
        // the missing fields.
        return new WsepPaymentRequest(
                amount.toPlainString(),
                DEFAULT_CURRENCY,
                details.getCardToken(),                  // pass cardToken as PAN
                /* month     */ 12,
                /* year      */ 2030,
                details.getBillingName(),
                /* cvv       */ "000",
                /* holder id */ details.getBillingName() // placeholder; a real id would come from the buyer
        );
    }
}
