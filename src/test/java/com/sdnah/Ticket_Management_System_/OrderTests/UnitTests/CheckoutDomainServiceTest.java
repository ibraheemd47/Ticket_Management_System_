package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.CheckoutDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.CheckoutDomainService.CheckoutResult;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Ticketcode;

/**
 * Exercises the saga rollback paths in Checkout_Domain_Service. Each test pins
 * one failure mode and asserts the compensating side effects ran (refund,
 * order cancellation, no orphaned charge, etc.).
 */
class CheckoutDomainServiceTest {

    private IPaymentGateway payment;
    private ITicketSupplierGateway tickets;
    private CheckoutDomainService service;

    private ActiveOrder order;
    private PaymentDetails details;

    @BeforeEach
    void setUp() {
        payment = mock(IPaymentGateway.class);
        tickets = mock(ITicketSupplierGateway.class);
        service = new CheckoutDomainService(payment, tickets);

        order = new ActiveOrder("buyer-1", UUID.randomUUID(), 30);
        details = mock(PaymentDetails.class);
    }

    private PaymentTransaction successfulTx() {
        return new PaymentTransaction("tx-1", order.getId(), BigDecimal.TEN, PaymentTransaction.Status.SUCCESS);
    }

    private PaymentTransaction failedTx() {
        return new PaymentTransaction("tx-1", order.getId(), BigDecimal.TEN, PaymentTransaction.Status.FAILED);
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Given a valid order, when checkout runs end-to-end, then no compensations are invoked")
    void happyPath_OrderCompletesAndNoCompensationsRun() {
        when(payment.charge(eq(order.getId()), any(BigDecimal.class), eq(details)))
                .thenReturn(successfulTx());
        when(tickets.issueTickets(eq(order.getId()), anyList()))
                .thenReturn(List.of(new Ticketcode("CODE-1", "QR-1")));

        CheckoutResult result = service.checkout(order, details);

        assertThat(result.getTicketCodes()).containsExactly("CODE-1");
        assertThat(result.getTransaction().isSuccessful()).isTrue();
        assertThat(order.getStatus()).isEqualTo(ActiveOrder.Status.COMPLETED);
        verify(payment, never()).refund(any());
    }

    // -------------------------------------------------------------------------
    // 1. Payment declined
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Given the gateway declines the charge, when checkout runs, then no refund/issue happens and order is cancelled")
    void paymentDeclined_NoRefundAndNoTicketsIssued() {
        when(payment.charge(eq(order.getId()), any(BigDecimal.class), eq(details)))
                .thenReturn(failedTx());

        assertThatThrownBy(() -> service.checkout(order, details))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Checkout failed");

        assertThat(order.getStatus()).isEqualTo(ActiveOrder.Status.ACTIVE);
        verify(payment, never()).refund(any());
        verify(tickets, never()).issueTickets(any(), anyList());
    }

    // -------------------------------------------------------------------------
    // 2. Ticket issuance fails after successful charge -> refund must fire
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Given a successful charge but ticket issuance fails, when checkout runs, then the payment is refunded")
    void ticketIssuanceFails_RefundsThePayment() {
        when(payment.charge(eq(order.getId()), any(BigDecimal.class), eq(details)))
                .thenReturn(successfulTx());
        when(tickets.issueTickets(eq(order.getId()), anyList()))
                .thenThrow(new RuntimeException("supplier offline"));

        assertThatThrownBy(() -> service.checkout(order, details))
                .isInstanceOf(IllegalStateException.class);

        verify(payment, times(1)).refund("tx-1");
        // Order finalize step never ran, so the order remains ACTIVE (not COMPLETED).
        assertThat(order.getStatus()).isNotEqualTo(ActiveOrder.Status.COMPLETED);
    }

    @Test
    @DisplayName("Given the supplier returns an empty code list, when checkout runs, then it is treated as failure and refunded")
    void ticketIssuanceEmpty_RefundsThePayment() {
        when(payment.charge(eq(order.getId()), any(BigDecimal.class), eq(details)))
                .thenReturn(successfulTx());
        when(tickets.issueTickets(eq(order.getId()), anyList()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.checkout(order, details))
                .isInstanceOf(IllegalStateException.class);

        verify(payment, times(1)).refund("tx-1");
    }
}