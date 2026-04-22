package com.sdnah.Ticket_Management_System_.Policy;

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SellingPolicy domain logic.
 * Verifies selection permissions for regular vs lottery-based sales[cite: 9].
 */
class SellingPolicyTest {

 

    @Test
    void givenRegularSellingPolicy_whenIsSelectionAllowed_thenReturnTrue() {
        SellingPolicy policy =
                new SellingPolicy(1, "selling policy", SellingPolicy.SellingType.REGULAR);

        boolean result = policy.isSelectionAllowed(false);

        assertTrue(result);
    }

    @Test
    void givenLotterySellingPolicyAndMember_whenIsSelectionAllowed_thenReturnTrue() {
        SellingPolicy policy =
                new SellingPolicy(1, "selling policy", SellingPolicy.SellingType.LOTTERY);

        boolean result = policy.isSelectionAllowed(true);

        assertTrue(result);
    }

    @Test
    void givenLotterySellingPolicyAndNonMember_whenIsSelectionAllowed_thenReturnFalse() {
        SellingPolicy policy =
                new SellingPolicy(1, "selling policy", SellingPolicy.SellingType.LOTTERY);

        boolean result = policy.isSelectionAllowed(false);

        assertFalse(result);
    }

    @Test
    void givenSellingPolicy_whenIsValidCalled_thenReturnTrueAsImplemented() {
        SellingPolicy policy =
                new SellingPolicy(1, "selling policy", SellingPolicy.SellingType.REGULAR);

        assertTrue(policy.isValid());
    }

    @Test
    void givenLotterySellingPolicy_whenCreated_thenObjectExists() {
        SellingPolicy policy =
                new SellingPolicy(7, "lottery", SellingPolicy.SellingType.LOTTERY);

        assertNotNull(policy);
        assertEquals(7, policy.getPolicyId());
        assertEquals("lottery", policy.getDescription());
    }
}