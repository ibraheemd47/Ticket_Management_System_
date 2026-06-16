package com.sdnah.Ticket_Management_System_.wsep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.CheckoutDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.CheckoutDomainService.CheckoutResult;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Ticket_Domain_Service;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepClient;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepTicketSupplierGateway;

/**
 * Saga-level integration of the checkout flow when the gateways are the WSEP
 * implementations (the production default). The {@link WsepClient} is mocked
 * so this test makes no real HTTP calls — but it exercises the full chain
 * {@code CheckoutDomainService → OrderSaga → WsepPaymentGateway /
 * WsepTicketSupplierGateway}, which the per-adapter unit tests don't.
 *
 * <p>Three things this test pins down:
 * <ol>
 *   <li>Happy path: WSEP returns success codes → saga finishes, result
 *       carries the WSEP transaction id and ticket code.</li>
 *   <li>Payment declined: WSEP {@code pay} returns -1 → saga fails, no
 *       ticket-issue call is made (the failed payment short-circuits).</li>
 *   <li>Ticket issue declined after payment success: saga must call
 *       {@code refund} on the just-charged WSEP transaction to compensate.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Checkout end-to-end with WSEP gateways (mocked transport)")
class CheckoutWithWsepIntegrationTest {

    @Mock private WsepClient wsepClient;
    @Mock private Ticket_Domain_Service ticketDomainService;

    private IPaymentGateway paymentGateway;
    private ITicketSupplierGateway ticketGateway;
    private CheckoutDomainService checkout;

    @BeforeEach
    void setUp() {
        paymentGateway = new WsepPaymentGateway(wsepClient);
        ticketGateway  = new WsepTicketSupplierGateway(wsepClient);
        checkout = new CheckoutDomainService(paymentGateway, ticketGateway, ticketDomainService);
    }

    // ── happy path ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("WSEP accepts payment + ticket → saga succeeds, result carries WSEP ids")
    void happyPath_chargeAndIssueSucceed() {
        when(wsepClient.pay(any())).thenReturn(54321);
        when(wsepClient.issueTicket(any())).thenReturn("TIX-abc-1234");

        ActiveOrder order = singleItemOrder();
        PaymentDetails details = new PaymentDetails("4242424242424242", "Alice", "VISA");

        CheckoutResult result = checkout.checkout(order, details);

        assertThat(result).isNotNull();
        assertThat(result.getTransaction().isSuccessful()).isTrue();
        assertThat(result.getTransaction().getTransactionId()).isEqualTo("54321");
        assertThat(result.getTicketCodes()).containsExactly("TIX-abc-1234");
    }

    // ── payment declined ────────────────────────────────────────────────────

    @Test
    @DisplayName("WSEP declines payment (-1) → saga rejects, no ticket-issue call is made")
    void paymentDeclined_abortsBeforeIssuingTickets() {
        when(wsepClient.pay(any())).thenReturn(WsepClient.FAILURE);

        ActiveOrder order = singleItemOrder();
        PaymentDetails details = new PaymentDetails("4242424242424242", "Alice", "VISA");

        assertThatThrownBy(() -> checkout.checkout(order, details))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Checkout failed");

        org.mockito.Mockito.verify(wsepClient, org.mockito.Mockito.never())
                .issueTicket(any());
    }

    // ── ticket issue fails after payment succeeds → refund must run ────────

    @Test
    @DisplayName("Ticket issue fails after payment success → saga refunds the charged WSEP transaction")
    void ticketIssueFails_refundsPayment() {
        when(wsepClient.pay(any())).thenReturn(54321);
        when(wsepClient.issueTicket(any())).thenReturn(null);   // WSEP returned "-1"
        when(wsepClient.refund(any())).thenReturn(1);            // refund acknowledged

        ActiveOrder order = singleItemOrder();
        PaymentDetails details = new PaymentDetails("4242424242424242", "Alice", "VISA");

        assertThatThrownBy(() -> checkout.checkout(order, details))
                .isInstanceOf(IllegalStateException.class);

        // The compensation step must call refund("54321") to undo the charge.
        org.mockito.Mockito.verify(wsepClient).refund("54321");
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static ActiveOrder singleItemOrder() {
        String buyerId = UUID.randomUUID().toString();
        UUID   eventId = UUID.randomUUID();
        UUID   areaId  = UUID.randomUUID();
        String ticketId = "TKT-" + UUID.randomUUID();

        ActiveOrder order = new ActiveOrder(buyerId, eventId, /* ttlMinutes */ 60);
        Lock lock = new Lock(ticketId, buyerId, LocalDateTime.now().plusMinutes(60));
        order.addTicket(ticketId, /* seatId */ null, areaId, new BigDecimal("100.00"), lock);
        return order;
    }
}
