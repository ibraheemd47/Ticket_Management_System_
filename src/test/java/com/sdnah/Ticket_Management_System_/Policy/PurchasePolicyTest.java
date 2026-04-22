package com.sdnah.Ticket_Management_System_.Policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PurchasePolicy logic.
 * Covers Use Case II.2.4 (Reserve) and II.2.8 (Checkout).
 */
class PurchasePolicyTest {
    private PurchasePolicy purchasePolicy;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize with default constraints (min: 1, max: Infinity, age: 0) 
        purchasePolicy = new PurchasePolicy(1, "General Purchase Policy");
    }

    @Test
    void GivenValidInput_WhenValidating_ThenReturnTrue() {
        // Act & Assert: Standard request (2 tickets, age 25) should pass 
        assertTrue(purchasePolicy.validatePurchase(2, 25, false));
    }

    @Test
    void GivenQuantityZero_WhenValidating_ThenReturnFalse() {
        // Act & Assert: Quantity below minTickets (1) should fail 
        assertFalse(purchasePolicy.validatePurchase(0, 20, false));
    }

    @Test
    void GivenNegativeAge_WhenValidating_ThenReturnFalse() {
        // Act & Assert: Age below minAge (0) should fail 
        assertFalse(purchasePolicy.validatePurchase(1, -5, false));
    }
}