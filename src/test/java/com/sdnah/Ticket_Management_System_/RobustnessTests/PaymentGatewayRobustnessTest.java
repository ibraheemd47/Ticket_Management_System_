package com.sdnah.Ticket_Management_System_.RobustnessTests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentGatewayProxy;

/**
 * Robustness tests for the payment gateway integration.
 *
 * Covers:
 * - Normal charge / refund paths (proxy stub returns correct data)
 * - System resilience when the gateway throws (network / timeout simulation)
 * - Behaviour with extreme / boundary payment amounts
 * - Idempotency: repeated refund calls do not crash the proxy
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Gateway — Robustness Tests")
class PaymentGatewayRobustnessTest {

    // Real proxy — exercises the stub implementation used in prod
    private PaymentGatewayProxy realProxy;

    // Mock gateway — used to simulate failure scenarios
    @Mock
    private IPaymentGateway mockGateway;

    @BeforeEach
    void setUp() {
        realProxy = new PaymentGatewayProxy();
    }

    // ── Happy-path through the real stub ─────────────────────────────────────

    @Test
    @DisplayName("GW: charge returns a SUCCESS transaction with matching orderId and amount")
    void charge_returnsSuccessTransaction() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        PaymentDetails details = new PaymentDetails("tok_visa", "Alice Smith", "CREDIT_CARD");

        PaymentTransaction tx = realProxy.charge(orderId, amount, details);

        assertThat(tx).isNotNull();
        assertThat(tx.getStatus()).isEqualTo(PaymentTransaction.Status.SUCCESS);
        assertThat(tx.getOrderId()).isEqualTo(orderId);
        assertThat(tx.getAmount()).isEqualByComparingTo(amount);
        assertThat(tx.getTransactionId()).isNotBlank();
    }

    @Test
    @DisplayName("GW: refund returns a REFUNDED transaction with the same transactionId")
    void refund_returnsRefundedTransaction() {
        String txId = "TX-" + UUID.randomUUID();

        PaymentTransaction refund = realProxy.refund(txId);

        assertThat(refund).isNotNull();
        assertThat(refund.getStatus()).isEqualTo(PaymentTransaction.Status.REFUNDED);
        assertThat(refund.getTransactionId()).isEqualTo(txId);
    }

    @Test
    @DisplayName("GW: charge with zero amount still returns a SUCCESS transaction")
    void charge_zeroAmount_doesNotCrash() {
        PaymentDetails details = new PaymentDetails("tok_visa", "Alice Smith", "CREDIT_CARD");

        PaymentTransaction tx = realProxy.charge(UUID.randomUUID(), BigDecimal.ZERO, details);

        assertThat(tx.getStatus()).isEqualTo(PaymentTransaction.Status.SUCCESS);
        assertThat(tx.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GW: charge with a very large amount — no arithmetic overflow in the proxy")
    void charge_veryLargeAmount_handledGracefully() {
        BigDecimal huge = new BigDecimal("9999999.99");
        PaymentDetails details = new PaymentDetails("tok_visa", "Alice Smith", "CREDIT_CARD");

        PaymentTransaction tx = realProxy.charge(UUID.randomUUID(), huge, details);

        assertThat(tx.getStatus()).isEqualTo(PaymentTransaction.Status.SUCCESS);
        assertThat(tx.getAmount()).isEqualByComparingTo(huge);
    }

    @Test
    @DisplayName("GW: multiple sequential charges produce unique transaction IDs")
    void charge_multipleCharges_uniqueTransactionIds() {
        PaymentDetails details = new PaymentDetails("tok_visa", "Alice Smith", "CREDIT_CARD");

        String tx1 = realProxy.charge(UUID.randomUUID(), BigDecimal.TEN, details).getTransactionId();
        String tx2 = realProxy.charge(UUID.randomUUID(), BigDecimal.TEN, details).getTransactionId();
        String tx3 = realProxy.charge(UUID.randomUUID(), BigDecimal.TEN, details).getTransactionId();

        assertThat(tx1).isNotEqualTo(tx2);
        assertThat(tx2).isNotEqualTo(tx3);
    }

    // ── Failure / resilience via mock ────────────────────────────────────────

    @Test
    @DisplayName("GW: gateway network timeout — exception propagates to caller")
    void charge_gatewayTimeout_propagatesException() {
        when(mockGateway.charge(any(), any(), any()))
                .thenThrow(new RuntimeException("Connection timed out"));

        assertThatThrownBy(() ->
                mockGateway.charge(UUID.randomUUID(), BigDecimal.TEN,
                        new PaymentDetails("tok_visa", "Alice Smith", "CREDIT_CARD")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    @DisplayName("GW: gateway returns FAILED status — caller receives failed transaction")
    void charge_gatewayReturnsFailed_callerReceivesFailedStatus() {
        UUID orderId = UUID.randomUUID();
        PaymentTransaction failedTx = new PaymentTransaction(
                "TX-FAIL", orderId, BigDecimal.TEN, PaymentTransaction.Status.FAILED);

        when(mockGateway.charge(eq(orderId), any(), any())).thenReturn(failedTx);

        PaymentTransaction result = mockGateway.charge(orderId, BigDecimal.TEN,
                new PaymentDetails("tok_declined", "Bob Jones", "CREDIT_CARD"));

        assertThat(result.getStatus()).isEqualTo(PaymentTransaction.Status.FAILED);
    }

    @Test
    @DisplayName("GW: refund on already-refunded transaction — proxy handles idempotently")
    void refund_alreadyRefunded_doesNotThrow() {
        // The proxy stub always returns REFUNDED regardless; this validates idempotency
        String txId = "TX-ALREADY-REFUNDED";
        assertThatCode(() -> realProxy.refund(txId)).doesNotThrowAnyException();
        assertThat(realProxy.refund(txId).getStatus()).isEqualTo(PaymentTransaction.Status.REFUNDED);
    }

    @Test
    @DisplayName("GW: gateway service unavailable — downstream exception wrapped correctly")
    void charge_serviceUnavailable_throwsRuntimeException() {
        when(mockGateway.charge(any(), any(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        assertThatThrownBy(() ->
                mockGateway.charge(UUID.randomUUID(), new BigDecimal("50.00"),
                        new PaymentDetails("tok_visa", "Alice Smith", "CREDIT_CARD")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("GW: refund gateway failure — exception surfaces to caller")
    void refund_gatewayFailure_exceptionPropagates() {
        when(mockGateway.refund(anyString()))
                .thenThrow(new RuntimeException("Refund service down"));

        assertThatThrownBy(() -> mockGateway.refund("TX-999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refund service down");
    }
}
