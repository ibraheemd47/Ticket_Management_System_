package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PurchaseContext — Value Object Unit Tests")
class PurchaseContextTest {

    @Test
    void GivenValidArgs_WhenConstructed_ThenAllFieldsAreStored() {
        LocalDateTime time = LocalDateTime.of(2026, 6, 1, 18, 0);
        PurchaseContext ctx = new PurchaseContext(25, 3, true, true, time);

        assertEquals(25, ctx.getBuyerAge());
        assertEquals(3, ctx.getTicketQuantity());
        assertTrue(ctx.isCreatesSingleSeatGap());
        assertTrue(ctx.isMember());
        assertEquals(time, ctx.getPurchaseTime());
    }

    @Test
    void GivenNullPurchaseTime_WhenConstructed_ThenDefaultsToNow() {
        PurchaseContext ctx = new PurchaseContext(20, 1, false, false, null);

        assertNotNull(ctx.getPurchaseTime());
    }

    @Test
    void GivenNegativeBuyerAge_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseContext(-1, 1, false, false, null));
    }

    @Test
    void GivenZeroTicketQuantity_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseContext(20, 0, false, false, null));
    }

    @Test
    void GivenNegativeTicketQuantity_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseContext(20, -5, false, false, null));
    }

    @Test
    void GivenZeroBuyerAge_WhenConstructed_ThenAccepted() {
        PurchaseContext ctx = new PurchaseContext(0, 1, false, false, null);

        assertEquals(0, ctx.getBuyerAge());
    }
}