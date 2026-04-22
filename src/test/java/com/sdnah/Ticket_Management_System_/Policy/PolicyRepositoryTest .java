package com.sdnah.Ticket_Management_System_.Policy;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


class PolicyRepositoryTest {

    private PolicyRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PolicyRepository();
    }

    @Test
    void givenPolicy_whenSaveAndFindById_thenReturnSamePolicy() {
        Policy policy = new PurchasePolicy(1, "purchase");

        repository.save(policy);
        Optional<Policy> result = repository.findById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getPolicyId());
        assertEquals("purchase", result.get().getDescription());
    }

    @Test
    void givenUnknownId_whenFindById_thenReturnEmptyOptional() {
        Optional<Policy> result = repository.findById(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void givenMultiplePolicies_whenFindAll_thenReturnAllSavedPolicies() {
        repository.save(new PurchasePolicy(1, "purchase"));
        repository.save(new DiscountPolicy(2, "discount"));
        repository.save(new SellingPolicy(3, "selling", SellingPolicy.SellingType.REGULAR));

        Collection<Policy> result = repository.findAll();

        assertEquals(3, result.size());
    }

    @Test
    void givenSavedPolicy_whenDeleteById_thenPolicyRemoved() {
        repository.save(new PurchasePolicy(1, "purchase"));

        repository.deleteById(1);

        assertTrue(repository.findById(1).isEmpty());
    }

    @Test
    void givenDeleteUnknownId_whenDeleteById_thenDoNothing() {
        assertDoesNotThrow(() -> repository.deleteById(999));
    }
}