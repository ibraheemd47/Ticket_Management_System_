package com.sdnah.Ticket_Management_System_.Policy;

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

/**
 * Unit tests for PurchasePolicy logic.
 * Covers Use Case II.2.4 (Reserve) and II.2.8 (Checkout).
 */

class PurchasePolicyTest {

    @Test
    void givenDefaultPolicy_whenValidatePurchaseWithValidInput_thenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        boolean result = policy.validatePurchase(2, 20, false);

        assertTrue(result);
    }

    @Test
    void givenQuantityBelowMinimum_whenValidatePurchase_thenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        boolean result = policy.validatePurchase(0, 20, false);

        assertFalse(result);
    }

    @Test
    void givenAgeBelowMinimum_whenValidatePurchase_thenReturnFalse() throws Exception {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        Field minAgeField = PurchasePolicy.class.getDeclaredField("minAge");
        minAgeField.setAccessible(true);
        minAgeField.set(policy, 18);

        boolean result = policy.validatePurchase(1, 16, false);

        assertFalse(result);
    }

    @Test
    void givenSingleSeatGapNotAllowed_whenValidatePurchaseAndGapCreated_thenReturnFalse() throws Exception {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        Field allowSingleSeatGapField = PurchasePolicy.class.getDeclaredField("allowSingleSeatGap");
        allowSingleSeatGapField.setAccessible(true);
        allowSingleSeatGapField.set(policy, false);

        boolean result = policy.validatePurchase(2, 20, true);

        assertFalse(result);
    }

    @Test
    void givenBoundaryQuantityEqualToMin_whenValidatePurchase_thenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        boolean result = policy.validatePurchase(1, 20, false);

        assertTrue(result);
    }

    @Test
    void givenBoundaryQuantityEqualToMax_whenValidatePurchase_thenReturnTrue() throws Exception {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        Field maxTicketsField = PurchasePolicy.class.getDeclaredField("maxTickets");
        maxTicketsField.setAccessible(true);
        maxTicketsField.set(policy, 5);

        boolean result = policy.validatePurchase(5, 20, false);

        assertTrue(result);
    }

    @Test
    void givenQuantityAboveMax_whenValidatePurchase_thenReturnFalse() throws Exception {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        Field maxTicketsField = PurchasePolicy.class.getDeclaredField("maxTickets");
        maxTicketsField.setAccessible(true);
        maxTicketsField.set(policy, 5);

        boolean result = policy.validatePurchase(6, 20, false);

        assertFalse(result);
    }

    @Test
    void givenValidPolicy_whenIsValid_thenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        assertTrue(policy.isValid());
    }

    @Test
    void givenMinTicketsGreaterThanMaxTickets_whenIsValid_thenReturnFalse() throws Exception {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        Field minTicketsField = PurchasePolicy.class.getDeclaredField("minTickets");
        Field maxTicketsField = PurchasePolicy.class.getDeclaredField("maxTickets");
        minTicketsField.setAccessible(true);
        maxTicketsField.setAccessible(true);

        minTicketsField.set(policy, 10);
        maxTicketsField.set(policy, 5);

        assertFalse(policy.isValid());
    }

    @Test
    void givenNegativeMinAge_whenIsValid_thenReturnFalse() throws Exception {
        PurchasePolicy policy = new PurchasePolicy(1, "purchase policy");

        Field minAgeField = PurchasePolicy.class.getDeclaredField("minAge");
        minAgeField.setAccessible(true);
        minAgeField.set(policy, -1);

        assertFalse(policy.isValid());
    }
}
