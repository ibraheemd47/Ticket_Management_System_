package com.sdnah.Ticket_Management_System_.Policy;

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SellingPolicy domain logic.
 * Verifies selection permissions for regular vs lottery-based sales[cite: 9].
 */
class SellingPolicyTest {

    /**
     * Scenario: Regular tickets are accessible to all guests and members[cite: 9].
     */
    @Test
    void GivenRegularSelling_WhenCheckingPermission_ThenAllowAll() 
    {
        SellingPolicy regular = new SellingPolicy(3, "Regular Sale", SellingPolicy.SellingType.REGULAR);
        assertTrue(regular.isSelectionAllowed(false), "Guest should be allowed"); 
        assertTrue(regular.isSelectionAllowed(true), "Member should be allowed"); 
    }

    /**
     * Scenario: Lottery tickets restrict Guest access (Use Case II.2.5 alternative flow).
     */
    @Test
    void GivenLotterySelling_WhenGuestRequests_ThenDenySelection() {
        SellingPolicy lottery = new SellingPolicy(4, "Lottery Sale", SellingPolicy.SellingType.LOTTERY);
        assertFalse(lottery.isSelectionAllowed(false), "Guests cannot directly select lottery tickets"); 
        assertTrue(lottery.isSelectionAllowed(true), "Members are eligible for lottery selection"); 
    }
}