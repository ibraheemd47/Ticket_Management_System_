package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.PurchasePolicy;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PurchasePolicy — Domain Unit Tests")
class PurchasePolicyTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Test
    void GivenDefaultPurchasePolicy_WhenQuantityOne_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID);

        boolean result = policy.validatePurchase(1, false);

        assertTrue(result);
    }

    @Test
    void GivenDefaultPurchasePolicy_WhenQuantityZero_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID);

        boolean result = policy.validatePurchase(0, false);

        assertFalse(result);
    }

    @Test
    void GivenDefaultPurchasePolicy_WhenQuantityNegative_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID);

        boolean result = policy.validatePurchase(-3, false);

        assertFalse(result);
    }

    @Test
    void GivenMaxTicketsTwo_WhenQuantityThree_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Max two", EVENT_ID);
        policy.setMaxTickets(2);

        boolean result = policy.validatePurchase(3, false);

        assertFalse(result);
    }

    @Test
    void GivenMinTicketsTwo_WhenQuantityOne_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Min two", EVENT_ID);
        policy.setMinTickets(2);

        boolean result = policy.validatePurchase(1, false);

        assertFalse(result);
    }

    @Test
    void GivenSingleSeatGapNotAllowed_WhenCreatesSingleGap_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "No single gap", EVENT_ID);
        policy.setAllowSingleSeatGap(false);

        boolean result = policy.validatePurchase(1, true);

        assertFalse(result);
    }

    @Test
    void GivenMinTicketsGreaterThanMaxTickets_WhenIsValid_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Invalid range", EVENT_ID);
        policy.setMinTickets(5);
        policy.setMaxTickets(2);

        assertFalse(policy.isValid());
    }
}