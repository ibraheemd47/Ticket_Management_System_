package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;

class TicketcodeTest {

    @Test
    void constructor_shouldStoreValuesCorrectly() {
        Ticketcode code = new Ticketcode("ABC123", "QRDATA");

        assertEquals("ABC123", code.getCode());
        assertEquals("QRDATA", code.getQrData());
    }

    @Test
    void constructor_shouldThrow_whenCodeIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Ticketcode("", "QRDATA")
        );

        assertNotNull(ex);
    }

    @Test
    void constructor_shouldThrow_whenQrDataIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Ticketcode("ABC123", "")
        );

        assertNotNull(ex);
    }
}