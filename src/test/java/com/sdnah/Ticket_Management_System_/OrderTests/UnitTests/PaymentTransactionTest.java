package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;

class PaymentTransactionTest {

    @Test
    void constructor_shouldStoreValuesCorrectly() {
        UUID orderId = UUID.randomUUID();

        PaymentTransaction tx = new PaymentTransaction(
                "tx1",
                orderId,
                new BigDecimal("50"),
                PaymentTransaction.Status.SUCCESS
        );

        assertEquals("tx1", tx.getTransactionId());
        assertEquals(orderId, tx.getOrderId());
        assertEquals(new BigDecimal("50"), tx.getAmount());
    }

    @Test
    void isSuccessful_shouldReturnTrue_whenStatusSuccess() {
        PaymentTransaction tx = new PaymentTransaction(
                "tx1",
                UUID.randomUUID(),
                new BigDecimal("50"),
                PaymentTransaction.Status.SUCCESS
        );

        assertTrue(tx.isSuccessful());
    }

    @Test
    void isSuccessful_shouldReturnFalse_whenStatusFailed() {
        PaymentTransaction tx = new PaymentTransaction(
                "tx1",
                UUID.randomUUID(),
                new BigDecimal("50"),
                PaymentTransaction.Status.FAILED
        );

        assertFalse(tx.isSuccessful());
    }

    @Test
    void markRefunded_shouldChangeStatus() {
        PaymentTransaction tx = new PaymentTransaction(
                "tx1",
                UUID.randomUUID(),
                new BigDecimal("50"),
                PaymentTransaction.Status.SUCCESS
        );

        tx.markRefunded();

        assertTrue(tx.isRefunded());
    }
}