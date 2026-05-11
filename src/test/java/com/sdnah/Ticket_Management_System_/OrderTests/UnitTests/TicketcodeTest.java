package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Ticketcode;

class TicketcodeTest {

    @Test
    void constructor_shouldStoreValuesCorrectly() {
        Ticketcode ticket = new Ticketcode("code123", "qrData123");

        assertEquals("code123", ticket.getCode());
        assertEquals("qrData123", ticket.getQrData());
    }

    @Test
    void constructor_shouldThrow_whenCodeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Ticketcode(null, "qrData")
        );

        assertEquals("code must not be empty", ex.getMessage());
    }

    @Test
    void constructor_shouldThrow_whenCodeIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Ticketcode("", "qrData")
        );

        assertEquals("code must not be empty", ex.getMessage());
    }

    @Test
    void constructor_shouldThrow_whenQrDataIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Ticketcode("code123", null)
        );

        assertEquals("qrData must not be empty", ex.getMessage());
    }

    @Test
    void constructor_shouldThrow_whenQrDataIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Ticketcode("code123", "")
        );

        assertEquals("qrData must not be empty", ex.getMessage());
    }
}