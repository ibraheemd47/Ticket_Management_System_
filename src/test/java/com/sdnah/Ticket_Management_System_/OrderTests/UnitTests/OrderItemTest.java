package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;

class OrderItemTest {

    @Test
    void isSeated_shouldReturnTrueWhenSeatIdExists() {
        OrderItem item = new OrderItem(1, 10, 5, new BigDecimal("50"));

        assertTrue(item.isSeated());
    }

    @Test
    void isSeated_shouldReturnFalseWhenSeatIdIsNull() {
        OrderItem item = new OrderItem(1, null, 5, new BigDecimal("50"));

        assertFalse(item.isSeated());
    }

    @Test
    void setLock_shouldSaveLock() {
        OrderItem item = new OrderItem(1, 10, 5, new BigDecimal("50"));
        Lock lock = new Lock(1, "buyer1", LocalDateTime.now().plusMinutes(5));

        item.setLock(lock);

        assertEquals(lock, item.getLock());
    }

    @Test
    void clearLock_shouldRemoveLock() {
        OrderItem item = new OrderItem(1, 10, 5, new BigDecimal("50"));
        Lock lock = new Lock(1, "buyer1", LocalDateTime.now().plusMinutes(5));

        item.setLock(lock);
        item.clearLock();

        assertNull(item.getLock());
    }

    @Test
    void constructor_shouldThrowExceptionWhenPriceIsNegative() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new OrderItem(1, 10, 5, new BigDecimal("-1"))
        );

        assertNotNull(ex);
    }
}