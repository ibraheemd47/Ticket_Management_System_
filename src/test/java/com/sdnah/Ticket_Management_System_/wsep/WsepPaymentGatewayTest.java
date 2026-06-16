package com.sdnah.Ticket_Management_System_.wsep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepClient;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepPaymentRequest;

/**
 * Unit tests for {@link WsepPaymentGateway}'s mapping logic. The HTTP layer
 * itself is covered by {@code WsepClientTest}; here we mock {@link WsepClient}
 * and verify the adapter converts domain types correctly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WsepPaymentGateway — Adapter mapping")
class WsepPaymentGatewayTest {

    @Mock private WsepClient client;

    private WsepPaymentGateway gateway;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("199.50");

    @BeforeEach
    void setUp() {
        gateway = new WsepPaymentGateway(client);
    }

    private static PaymentDetails details() {
        return new PaymentDetails("4242424242424242", "Alice Buyer", "VISA");
    }

    // ── charge ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("charge maps amount + PaymentDetails into the WSEP request")
    void charge_buildsRequestFromDomainTypes() {
        when(client.pay(any())).thenReturn(54321);

        gateway.charge(ORDER_ID, AMOUNT, details());

        ArgumentCaptor<WsepPaymentRequest> captor = ArgumentCaptor.forClass(WsepPaymentRequest.class);
        org.mockito.Mockito.verify(client).pay(captor.capture());
        WsepPaymentRequest sent = captor.getValue();

        assertThat(sent.amount()).isEqualTo("199.50");
        assertThat(sent.currency()).isEqualTo("USD");
        assertThat(sent.cardNumber()).isEqualTo("4242424242424242");
        assertThat(sent.holder()).isEqualTo("Alice Buyer");
    }

    @Test
    @DisplayName("charge returns a SUCCESS PaymentTransaction carrying WSEP's id")
    void charge_successMappedToSuccessfulTxn() {
        when(client.pay(any())).thenReturn(54321);

        PaymentTransaction txn = gateway.charge(ORDER_ID, AMOUNT, details());

        assertThat(txn.isSuccessful()).isTrue();
        assertThat(txn.getTransactionId()).isEqualTo("54321");
        assertThat(txn.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(txn.getAmount()).isEqualTo(AMOUNT);
    }

    @Test
    @DisplayName("charge returns a FAILED PaymentTransaction when WSEP returns -1")
    void charge_declinedMappedToFailedTxn() {
        when(client.pay(any())).thenReturn(WsepClient.FAILURE);

        PaymentTransaction txn = gateway.charge(ORDER_ID, AMOUNT, details());

        assertThat(txn.isSuccessful()).isFalse();
        assertThat(txn.getStatus()).isEqualTo(PaymentTransaction.Status.FAILED);
        assertThat(txn.getTransactionId()).startsWith("wsep-fail-");
    }

    @Test
    @DisplayName("charge rejects zero/negative amounts up front (no WSEP call)")
    void charge_rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> gateway.charge(ORDER_ID, BigDecimal.ZERO, details()))
                .isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.verifyNoInteractions(client);
    }

    // ── refund ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refund marks the returned PaymentTransaction REFUNDED on success")
    void refund_successMappedToRefundedTxn() {
        when(client.refund(eq("12345"))).thenReturn(1);

        PaymentTransaction refund = gateway.refund("12345");

        assertThat(refund.isRefunded()).isTrue();
        assertThat(refund.getTransactionId()).isEqualTo("12345");
    }

    @Test
    @DisplayName("refund marks the returned PaymentTransaction FAILED when WSEP refuses")
    void refund_failureMappedToFailedTxn() {
        when(client.refund(eq("12345"))).thenReturn(WsepClient.FAILURE);

        PaymentTransaction refund = gateway.refund("12345");

        assertThat(refund.getStatus()).isEqualTo(PaymentTransaction.Status.FAILED);
    }
}
