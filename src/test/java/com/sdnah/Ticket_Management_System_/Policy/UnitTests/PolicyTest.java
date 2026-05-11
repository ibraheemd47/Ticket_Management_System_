package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.PurchasePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Policy — Base Class Unit Tests")
class PolicyTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Test
    void GivenNegativePolicyId_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(-1, "Invalid", EVENT_ID)
        );
    }

    @Test
    void GivenZeroPolicyId_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(0, "Invalid", EVENT_ID)
        );
    }

    @Test
    void GivenNullDescription_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(1, null, EVENT_ID)
        );
    }

    @Test
    void GivenBlankDescription_WhenCreatePolicy_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new PurchasePolicy(1, "   ", EVENT_ID)
        );
    }

    @Test
    void GivenValidPolicy_WhenGettersCalled_ThenReturnConstructorValues() {
        PurchasePolicy policy = new PurchasePolicy(1, "Valid", EVENT_ID);

        assertEquals(1, policy.getPolicyId());
        assertEquals("Valid", policy.getDescription());
        assertEquals(EVENT_ID, policy.getEventId());
    }

    @Test
    void GivenNullEventId_WhenCreatePolicy_ThenPolicyCreatedAsDefaultPolicy() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default company policy", null);

        assertNull(policy.getEventId());
    }

    @Test
    void GivenValidPolicy_WhenIsValid_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Valid", EVENT_ID);

        assertTrue(policy.isValid());
    }
}