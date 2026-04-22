package com.sdnah.Ticket_Management_System_.Policy;


import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PolicyServiceTest {

    private IPolicyRepo policyRepo;
    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyRepo = new InMemoryPolicyRepoForTest();
        policyService = new PolicyService(policyRepo);
    }

    @Test
    void givenRegularSellingPolicy_whenCheckSelectionPermission_thenReturnTrue() {
        policyRepo.save(new SellingPolicy(1, "selling", SellingPolicy.SellingType.REGULAR));

        boolean result = policyService.checkSelectionPermission(1, false);

        assertTrue(result);
    }

    @Test
    void givenLotterySellingPolicyAndNotMember_whenCheckSelectionPermission_thenReturnFalse() {
        policyRepo.save(new SellingPolicy(1, "selling", SellingPolicy.SellingType.LOTTERY));

        boolean result = policyService.checkSelectionPermission(1, false);

        assertFalse(result);
    }

    @Test
    void givenMissingSellingPolicy_whenCheckSelectionPermission_thenReturnTrueByDefault() {
        boolean result = policyService.checkSelectionPermission(999, false);

        assertTrue(result);
    }

    @Test
    void givenValidPurchasePolicy_whenValidateReservationRequest_thenReturnTrue() {
        policyRepo.save(new PurchasePolicy(2, "purchase"));

        boolean result = policyService.validateReservationRequest(2, 2, 20);

        assertTrue(result);
    }

    @Test
    void givenInvalidPurchaseRequest_whenValidateReservationRequest_thenReturnFalse() {
        policyRepo.save(new PurchasePolicy(2, "purchase"));

        boolean result = policyService.validateReservationRequest(2, 0, 20);

        assertFalse(result);
    }

    @Test
    void givenMissingPurchasePolicy_whenValidateReservationRequest_thenReturnTrueByDefault() {
        boolean result = policyService.validateReservationRequest(999, 2, 20);

        assertTrue(result);
    }

    @Test
    void givenNoRelevantPolicies_whenProcessCheckout_thenReturnBasePrice() {
        double result = policyService.processCheckout(999, 999, 100.0, 2, 20, null);

        assertEquals(100.0, result, 0.0001);
    }

    @Test
    void givenPurchasePolicyViolationAtCheckout_whenProcessCheckout_thenReturnMinusOne() {
        policyRepo.save(new PurchasePolicy(1, "purchase"));

        double result = policyService.processCheckout(1, 999, 100.0, 0, 20, null);

        assertEquals(-1.0, result, 0.0001);
    }

    @Test
    void givenMissingDiscountPolicyAndValidPurchasePolicy_whenProcessCheckout_thenReturnBasePrice() {
        policyRepo.save(new PurchasePolicy(1, "purchase"));

        double result = policyService.processCheckout(1, 999, 150.0, 2, 20, null);

        assertEquals(150.0, result, 0.0001);
    }

    private static class InMemoryPolicyRepoForTest implements IPolicyRepo {
        private final Map<Integer, Policy> policies = new HashMap<>();

        @Override
        public void save(Policy policy) {
            policies.put(policy.getPolicyId(), policy);
        }

        @Override
        public Optional<Policy> findById(int policyId) {
            return Optional.ofNullable(policies.get(policyId));
        }

        @Override
        public Collection<Policy> findAll() {
            return policies.values();
        }

        @Override
        public void deleteById(int policyId) {
            policies.remove(policyId);
        }
    }
}