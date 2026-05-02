package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;

@DisplayName("Policy — Base Class Unit Tests")
class PolicyTest {

    @Test
    void GivenNegativePolicyId_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(-1, "Invalid", 20, 10)
        );
    }

    @Test
    void GivenZeroPolicyId_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(0, "Invalid", 20, 10)
        );
    }

    @Test
    void GivenNegativeCompanyId_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(1, "Invalid", 20, -10)
        );
    }

    @Test
    void GivenZeroCompanyId_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(1, "Invalid", 20, 0)
        );
    }

    @Test
    void GivenNullDescription_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(1, null, 20, 10)
        );
    }

    @Test
    void GivenBlankDescription_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(1, "   ", 20, 10)
        );
    }

    @Test
    void GivenValidPolicy_WhenGettersCalled_ThenReturnConstructorValues() {
        PurchasePolicy policy = new PurchasePolicy(1, "Valid", 20, 10);

        assertEquals(1, policy.getPolicyId());
        assertEquals("Valid", policy.getDescription());
        assertEquals(20, policy.getEventId());
        assertEquals(10, policy.getCompanyId());
    }
}