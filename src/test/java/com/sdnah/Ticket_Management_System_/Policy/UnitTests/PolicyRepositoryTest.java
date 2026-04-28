package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository;

@DisplayName("PolicyRepository — Unit Tests")
class PolicyRepositoryTest {

    private static class FakePolicy extends Policy {
        public FakePolicy(int id) {
            super(id, "test", 100);
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    @Test
    @DisplayName("Given new policy, when saving, then it can be retrieved by id")
    void givenNewPolicy_WhenSaving_ThenCanBeRetrieved() {
        // Arrange
        PolicyRepository repo = new PolicyRepository();
        Policy policy = new FakePolicy(1);

        // Act
        repo.save(policy);

        // Assert
        assertTrue(repo.findById(1).isPresent());
    }

    @Test
    @DisplayName("Given saved policy, when deleting, then it is removed")
    void givenSavedPolicy_WhenDeleting_ThenRemoved() {
        // Arrange
        PolicyRepository repo = new PolicyRepository();
        Policy policy = new FakePolicy(2);
        repo.save(policy);

        // Act
        repo.deleteById(2);

        // Assert
        assertTrue(repo.findById(2).isEmpty());
    }

    @Test
    @DisplayName("Given multiple policies, when findAll is called, then all policies are returned")
    void givenMultiplePolicies_WhenFindAll_ThenAllReturned() {
        // Arrange
        PolicyRepository repo = new PolicyRepository();
        repo.save(new FakePolicy(1));
        repo.save(new FakePolicy(2));

        // Act
        Collection<Policy> result = repo.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Given non-existing policy, when findById is called, then empty optional is returned")
    void givenNonExistingPolicy_WhenFindById_ThenEmpty() {
        // Arrange
        PolicyRepository repo = new PolicyRepository();

        // Act
        var result = repo.findById(999);

        // Assert
        assertTrue(result.isEmpty());
    }
}