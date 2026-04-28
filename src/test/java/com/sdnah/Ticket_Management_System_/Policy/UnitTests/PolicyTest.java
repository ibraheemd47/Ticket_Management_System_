package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;

@DisplayName("Policy — Domain Unit Tests")
class PolicyTest {

    private static class FakePolicy extends Policy {
        private final boolean valid;

        FakePolicy(int policyId, String description, int eventId, boolean valid) {
            super(policyId, description, eventId);
            this.valid = valid;
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }

    @Test
    @DisplayName("Given valid policy data, when policy is constructed, then id is stored correctly")
    void givenValidPolicyData_WhenConstructed_ThenIdIsStoredCorrectly() {
        // Arrange
        int policyId = 1;

        // Act
        Policy policy = new FakePolicy(policyId, "Test policy", 100, true);

        // Assert
        assertEquals(policyId, policy.getPolicyId());
    }

    @Test
    @DisplayName("Given valid policy data, when policy is constructed, then description is stored correctly")
    void givenValidPolicyData_WhenConstructed_ThenDescriptionIsStoredCorrectly() {
        // Arrange
        String description = "Test policy";

        // Act
        Policy policy = new FakePolicy(1, description, 100, true);

        // Assert
        assertEquals(description, policy.getDescription());
    }

    @Test
    @DisplayName("Given valid policy data, when policy is constructed, then event id is stored correctly")
    void givenValidPolicyData_WhenConstructed_ThenEventIdIsStoredCorrectly() {
        // Arrange
        int eventId = 100;

        // Act
        Policy policy = new FakePolicy(1, "Test policy", eventId, true);

        // Assert
        assertEquals(eventId, policy.getEventId());
    }

    @Test
    @DisplayName("Given valid fake policy, when isValid is called, then returns true")
    void givenValidFakePolicy_WhenIsValidCalled_ThenReturnsTrue() {
        // Arrange
        Policy policy = new FakePolicy(1, "Valid policy", 100, true);

        // Act
        boolean result = policy.isValid();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given invalid fake policy, when isValid is called, then returns false")
    void givenInvalidFakePolicy_WhenIsValidCalled_ThenReturnsFalse() {
        // Arrange
        Policy policy = new FakePolicy(1, "Invalid policy", 100, false);

        // Act
        boolean result = policy.isValid();

        // Assert
        assertFalse(result);
    }
}