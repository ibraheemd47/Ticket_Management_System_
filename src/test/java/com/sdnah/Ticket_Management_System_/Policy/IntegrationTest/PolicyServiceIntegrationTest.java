package com.sdnah.Ticket_Management_System_.Policy.IntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;

class PolicyServiceIntegrationTest {

    private static final int COMPANY_ID = 10;
    private static final int EVENT_ID = 20;

    private PolicyService policyService;
    private InMemoryPolicyRepo policyRepo;

    static class InMemoryPolicyRepo implements IPolicyRepo {
        private final Map<Integer, Policy> store = new HashMap<>();

        @Override
        public void save(Policy policy) {
            store.put(policy.getPolicyId(), policy);
        }

        @Override
        public Optional<Policy> findById(int policyId) {
            return Optional.ofNullable(store.get(policyId));
        }

        @Override
        public Collection<Policy> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public void deleteById(int policyId) {
            store.remove(policyId);
        }
    }

    @BeforeEach
    void setUp() {
        policyRepo = new InMemoryPolicyRepo();
        policyService = new PolicyService(policyRepo);
    }

    @Test
    @DisplayName("Given regular selling policy, when guest selects tickets, then selection is approved")
    void givenRegularSellingPolicy_WhenGuestSelectsTickets_ThenSelectionIsApproved() {
        policyRepo.save(new SellingPolicy(1, "Regular policy",
                SellingPolicy.SellingType.REGULAR, EVENT_ID, COMPANY_ID));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given lottery selling policy, when guest selects tickets, then selection is rejected")
    void givenLotterySellingPolicy_WhenGuestSelectsTickets_ThenSelectionIsRejected() {
        policyRepo.save(new SellingPolicy(2, "Lottery policy",
                SellingPolicy.SellingType.LOTTERY, EVENT_ID, COMPANY_ID));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);

        assertFalse(result);
    }

    @Test
    @DisplayName("Given lottery selling policy, when member selects tickets, then selection is approved")
    void givenLotterySellingPolicy_WhenMemberSelectsTickets_ThenSelectionIsApproved() {
        policyRepo.save(new SellingPolicy(3, "Lottery policy",
                SellingPolicy.SellingType.LOTTERY, EVENT_ID, COMPANY_ID));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, true);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given no selling policy, when checking selection, then default approves selection")
    void givenNoSellingPolicy_WhenCheckingSelection_ThenDefaultApprovesSelection() {
        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given purchase policy, when validating valid reservation, then approved")
    void givenPurchasePolicy_WhenValidatingValidReservation_ThenApproved() {
        policyRepo.save(new PurchasePolicy(4, "Standard purchase", EVENT_ID, COMPANY_ID));

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 2, 25);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given no discount policy, when applying general discounts, then base price is unchanged")
    void givenNoDiscountPolicy_WhenApplyingGeneralDiscounts_ThenBasePriceIsUnchanged() {
        double result = policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 1);

        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Given discount policy with percentage rule, when applying general discounts, then price is reduced")
    void givenDiscountPolicyWithPercentageRule_WhenApplyingGeneralDiscounts_ThenPriceIsReduced() {
        DiscountPolicy discountPolicy = new DiscountPolicy(5, "10 percent off", EVENT_ID, COMPANY_ID);
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));
        policyRepo.save(discountPolicy);

        double result = policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 1);

        assertEquals(90.0, result);
    }

    @Test
    @DisplayName("Given coupon discount, when applying matching coupon, then discount is applied")
    void givenCouponDiscount_WhenApplyingMatchingCoupon_ThenDiscountIsApplied() {
        DiscountPolicy discountPolicy = new DiscountPolicy(6, "Coupon SAVE10", EVENT_ID, COMPANY_ID);
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE10", 10.0));
        policyRepo.save(discountPolicy);

        double result = policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 1, "SAVE10");

        assertEquals(90.0, result);
    }

    @Test
    @DisplayName("Given conditional discount with quantity threshold met, when checking, then satisfied")
    void givenConditionalDiscountWithQuantityThresholdMet_WhenChecking_ThenSatisfied() {
        DiscountPolicy discountPolicy = new DiscountPolicy(7, "Buy 3 get discount", EVENT_ID, COMPANY_ID);
        discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 20.0));
        policyRepo.save(discountPolicy);

        assertTrue(policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 3));
        assertFalse(policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 2));
    }

    @Test
    @DisplayName("Given event-specific policy preferred over company policy, when both exist, then event policy wins")
    void givenEventSpecificPolicyPreferredOverCompanyPolicy_WhenBothExist_ThenEventPolicyWins() {
        policyRepo.save(new SellingPolicy(8, "Company-wide lottery",
                SellingPolicy.SellingType.LOTTERY, null, COMPANY_ID));
        policyRepo.save(new SellingPolicy(9, "Event regular",
                SellingPolicy.SellingType.REGULAR, EVENT_ID, COMPANY_ID));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given changePurchasePolicy called, when invoked, then UnsupportedOperationException is thrown")
    void givenChangePurchasePolicyCalled_WhenInvoked_ThenUnsupportedOperationExceptionIsThrown() {
        assertThrows(UnsupportedOperationException.class,
                () -> policyService.changePurchasePolicy());
    }
}
