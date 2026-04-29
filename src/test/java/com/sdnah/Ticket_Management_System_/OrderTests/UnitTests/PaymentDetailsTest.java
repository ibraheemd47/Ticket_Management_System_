package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;

class PaymentDetailsTest {

    @Test
    void constructor_shouldStoreValuesCorrectly() {
        PaymentDetails details =
                new PaymentDetails("token123", "buyer1", "VISA");

        assertEquals("token123", details.getCardToken());
        assertEquals("buyer1", details.getBillingName());
        assertEquals("VISA", details.getPaymentMethod());
    }

    @Test
    void constructor_shouldThrow_whenCardTokenIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentDetails("", "buyer1", "VISA")
        );

        assertNotNull(ex);
    }

    @Test
    void constructor_shouldThrow_whenBillingNameIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentDetails("token123", "", "VISA")
        );

        assertNotNull(ex);
    }

    @Test
    void constructor_shouldThrow_whenPaymentMethodIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentDetails("token123", "buyer1", "")
        );

        assertNotNull(ex);
    }
}