package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;

@DisplayName("PurchasePolicy — Domain Unit Tests")
class PurchasePolicyTest {

    private static final int COMPANY_ID = 10;
    private static final int EVENT_ID = 20;

    @Test
    void GivenDefaultPurchasePolicy_WhenQuantityOne_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID, COMPANY_ID);

        boolean result = policy.validatePurchase(1, 18, false);

        assertTrue(result);
    }

    @Test
    void GivenDefaultPurchasePolicy_WhenQuantityZero_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID, COMPANY_ID);

        boolean result = policy.validatePurchase(0, 18, false);

        assertFalse(result);
    }

    @Test
    void GivenDefaultPurchasePolicy_WhenQuantityNegative_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID, COMPANY_ID);

        boolean result = policy.validatePurchase(-3, 18, false);

        assertFalse(result);
    }

    @Test
    void GivenDefaultPurchasePolicy_WhenVeryLargeQuantity_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID, COMPANY_ID);

        boolean result = policy.validatePurchase(Integer.MAX_VALUE, 18, false);

        assertTrue(result);
    }

    @Test
    void GivenDefaultPurchasePolicy_WhenUserAgeZero_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID, COMPANY_ID);

        boolean result = policy.validatePurchase(1, 0, false);

        assertTrue(result);
    }

    @Test
    void GivenDefaultPurchasePolicy_WhenCreatesSingleGap_ThenReturnTrueBecauseAllowedByDefault() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID, COMPANY_ID);

        boolean result = policy.validatePurchase(1, 18, true);

        assertTrue(result);
    }

    @Test
    void GivenDefaultPurchasePolicy_WhenIsValid_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID, COMPANY_ID);

        assertTrue(policy.isValid());
    }
}