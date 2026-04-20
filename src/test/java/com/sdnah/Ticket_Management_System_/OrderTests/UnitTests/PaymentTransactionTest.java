package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;

class PaymentTransactionTest {

    @Test
    void isSuccessful_shouldReturnTrue_whenStatusIsSuccess() {
        PaymentTransaction tx =
                new PaymentTransaction("tx1", 1, new BigDecimal("100"), PaymentTransaction.Status.SUCCESS);

        assertTrue(tx.isSuccessful());
        assertFalse(tx.isRefunded());
    }

    @Test
    void markRefunded_shouldChangeStatusToRefunded() {
        PaymentTransaction tx =
                new PaymentTransaction("tx1", 1, new BigDecimal("100"), PaymentTransaction.Status.SUCCESS);

        tx.markRefunded();

        assertTrue(tx.isRefunded());
        assertEquals(PaymentTransaction.Status.REFUNDED, tx.getStatus());
    }

    @Test
    void constructor_shouldThrow_whenTransactionIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentTransaction("", 1, new BigDecimal("100"), PaymentTransaction.Status.SUCCESS)
        );

        assertNotNull(ex);
    }

    @Test
    void constructor_shouldThrow_whenOrderIdIsInvalid() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentTransaction("tx1", 0, new BigDecimal("100"), PaymentTransaction.Status.SUCCESS)
        );

        assertNotNull(ex);
    }

    @Test
    void constructor_shouldThrow_whenAmountIsNegative() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentTransaction("tx1", 1, new BigDecimal("-1"), PaymentTransaction.Status.SUCCESS)
        );

        assertNotNull(ex);
    }

    @Test
    void constructor_shouldThrow_whenStatusIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentTransaction("tx1", 1, new BigDecimal("100"), null)
        );

        assertNotNull(ex);
    }
}