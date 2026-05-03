package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;

class LockTest {

    @Test
    void constructor_shouldStoreValuesCorrectly() {
        LocalDateTime future = LocalDateTime.now().plusMinutes(10);

        Lock lock = new Lock("ticket1", "buyer1", future);

        assertEquals("ticket1", lock.getResourceId());
        assertEquals("buyer1", lock.getOwnerId());
        assertEquals(future, lock.getExpiresAt());
    }

    @Test
    void isExpired_shouldReturnFalse_whenNotExpired() {
        LocalDateTime future = LocalDateTime.now().plusMinutes(10);
        Lock lock = new Lock("ticket1", "buyer1", future);

        assertFalse(lock.isExpired());
    }

    @Test
    void isExpired_shouldReturnTrue_whenExpired() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(10);
        Lock lock = new Lock("ticket1", "buyer1", past);

        assertTrue(lock.isExpired());
    }

    @Test
    void isHeldBy_shouldReturnTrue_forSameOwner() {
        Lock lock = new Lock("ticket1", "buyer1", LocalDateTime.now().plusMinutes(10));

        assertTrue(lock.isHeldBy("buyer1"));
    }

    @Test
    void isHeldBy_shouldReturnFalse_forDifferentOwner() {
        Lock lock = new Lock("ticket1", "buyer1", LocalDateTime.now().plusMinutes(10));

        assertFalse(lock.isHeldBy("buyer2"));
    }
}