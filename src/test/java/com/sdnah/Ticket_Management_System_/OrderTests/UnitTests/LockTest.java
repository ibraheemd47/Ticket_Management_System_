package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;

class LockTest {

    @Test
    void isExpired_shouldReturnTrue_whenTimePassed() {
        Lock lock = new Lock(1, "buyer1", LocalDateTime.now().minusMinutes(1));

        assertTrue(lock.isExpired());
    }

    @Test
    void isExpired_shouldReturnFalse_whenStillValid() {
        Lock lock = new Lock(1, "buyer1", LocalDateTime.now().plusMinutes(5));

        assertFalse(lock.isExpired());
    }

    @Test
    void isHeldBy_shouldReturnTrue_forCorrectBuyer() {
        Lock lock = new Lock(1, "buyer1", LocalDateTime.now().plusMinutes(5));

        assertTrue(lock.isHeldBy("buyer1"));
        assertFalse(lock.isHeldBy("buyer2"));
    }

    @Test
    void constructor_shouldThrow_whenResourceIdInvalid() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Lock(0, "buyer1", LocalDateTime.now().plusMinutes(5))
        );

        assertNotNull(ex);
    }

    @Test
    void constructor_shouldThrow_whenBuyerIdEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Lock(1, "", LocalDateTime.now().plusMinutes(5))
        );

        assertNotNull(ex);
    }

    @Test
    void constructor_shouldThrow_whenExpiresAtNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Lock(1, "buyer1", null)
        );

        assertNotNull(ex);
}
}