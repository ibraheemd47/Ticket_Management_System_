package com.sdnah.Ticket_Management_System_.Policy;   

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;

import static org.junit.jupiter.api.Assertions.*;



class PolicyTest {

    private static class TestPolicy extends Policy {
        private final boolean valid;

        public TestPolicy(int policyId, String description, boolean valid) {
            super(policyId, description);
            this.valid = valid;
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }

    @Test
    void givenPolicy_whenGettersCalled_thenReturnCorrectValues() {
        TestPolicy policy = new TestPolicy(10, "test policy", true);

        assertEquals(10, policy.getPolicyId());
        assertEquals("test policy", policy.getDescription());
    }

    @Test
    void givenConcreteSubclass_whenIsValidCalled_thenReturnProvidedValue() {
        TestPolicy validPolicy = new TestPolicy(1, "valid", true);
        TestPolicy invalidPolicy = new TestPolicy(2, "invalid", false);

        assertTrue(validPolicy.isValid());
        assertFalse(invalidPolicy.isValid());
    }
}