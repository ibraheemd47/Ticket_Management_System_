package com.sdnah.Ticket_Management_System_.Policy.IntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

/**
 * Integration tests for the Policy module.
 * Persists policies through the real PolicyRepository, then re-loads them to
 * verify JPA mappings + composite rule serialization survive the round-trip.
 * This is a repository-focused integration test; service-level orchestration
 * is covered separately in PolicyServiceAcceptanceTest with mocks.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Policy Module — Integration Tests")
class PolicyServiceIntegrationTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();

    @Autowired private PolicyRepository policyRepository;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
        eventId = UUID.randomUUID();
    }

    // =========================================================================
    // Discount Policy — persistence round-trip
    // =========================================================================

    @Test
    @DisplayName("Given discount policy with rule, when loaded, then discount applied")
    void givenDiscountPolicyWithRule_WhenLoaded_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(1, "10% off", eventId, COMPANY_ID);
        policy.addRule(new PercentageDiscountRule(10.0, "10% off"));
        policyRepository.saveAndFlush(policy);

        DiscountPolicy loaded = policyRepository
                .findDiscountPolicyByEventId(eventId).orElseThrow();

        assertEquals(90.0, loaded.computeFinalPrice(100.0,
                new DiscountContext(1, null)), 0.001);
    }

    @Test
    @DisplayName("Given coupon rule, when correct coupon, then discount applied")
    void givenCouponRule_WhenCorrectCoupon_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(2, "Coupon SAVE10", eventId, COMPANY_ID);
        policy.addRule(new CouponDiscountRule(10.0, "SAVE10"));
        policyRepository.saveAndFlush(policy);

        DiscountPolicy loaded = policyRepository
                .findDiscountPolicyByEventId(eventId).orElseThrow();

        assertEquals(90.0, loaded.calculateFinalPrice(100.0, 1, "SAVE10"), 0.001);
        assertEquals(100.0, loaded.calculateFinalPrice(100.0, 1, "WRONG"), 0.001);
    }

    @Test
    @DisplayName("Given conditional rule, when quantity meets threshold, then discount applied")
    void givenConditionalRule_WhenQuantityMeets_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(3, "Buy 3 get 20%", eventId, COMPANY_ID);
        policy.addRule(new QuantityConditionalDiscountRule(3, 20.0));
        policyRepository.saveAndFlush(policy);

        DiscountPolicy loaded = policyRepository
                .findDiscountPolicyByEventId(eventId).orElseThrow();

        assertEquals(80.0, loaded.calculateFinalPrice(100.0, 3, ""), 0.001);
        assertEquals(100.0, loaded.calculateFinalPrice(100.0, 2, ""), 0.001);
    }

    @Test
    @DisplayName("Given composite discount policy, when loaded, then composite tree is preserved")
    void givenCompositeDiscountPolicy_WhenLoaded_ThenCompositeTreePreserved() {
        DiscountPolicy policy = new DiscountPolicy(13, "Best of two", eventId, COMPANY_ID);
        policy.setRules(List.of(
                        new PercentageDiscountRule(10.0, "10% off"),
                        new PercentageDiscountRule(25.0, "25% off")),
                false); // MAX
        policyRepository.saveAndFlush(policy);

        DiscountPolicy loaded = policyRepository
                .findDiscountPolicyByEventId(eventId).orElseThrow();

        // best-of MAX → 25%, so 100 * 0.75 = 75
        assertEquals(75.0, loaded.computeFinalPrice(100.0,
                new DiscountContext(1, null)), 0.001);
    }

    @Test
    @DisplayName("Given additive composite discount policy, when loaded, then sum is preserved")
    void givenAdditiveCompositeDiscountPolicy_WhenLoaded_ThenSumPreserved() {
        DiscountPolicy policy = new DiscountPolicy(14, "Additive", eventId, COMPANY_ID);
        policy.setRules(List.of(
                        new PercentageDiscountRule(10.0, "10% off"),
                        new PercentageDiscountRule(20.0, "20% off")),
                true); // SUM
        policyRepository.saveAndFlush(policy);

        DiscountPolicy loaded = policyRepository
                .findDiscountPolicyByEventId(eventId).orElseThrow();

        // additive SUM → 30%, so 100 * 0.7 = 70
        assertEquals(70.0, loaded.computeFinalPrice(100.0,
                new DiscountContext(1, null)), 0.001);
    }

    @Test
    @DisplayName("Given no discount policy, when queried, then empty returned")
    void givenNoDiscountPolicy_WhenQueried_ThenEmpty() {
        assertTrue(policyRepository.findDiscountPolicyByEventId(eventId).isEmpty());
    }

    // =========================================================================
    // Purchase Policy — persistence round-trip
    // =========================================================================

    @Test
    @DisplayName("Given purchase policy with no rules, when validated, then approved")
    void givenPurchasePolicyNoRules_WhenValidated_ThenApproved() {
        policyRepository.saveAndFlush(
                new PurchasePolicy(4, "Default", eventId, COMPANY_ID));

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByEventId(eventId).orElseThrow();

        assertTrue(loaded.validatePurchase(2, 25, false));
    }

    @Test
    @DisplayName("Given MinAgeRule, when buyer too young, then rejected")
    void givenMinAgeRule_WhenBuyerTooYoung_ThenRejected() {
        PurchasePolicy policy = new PurchasePolicy(5, "18+ policy", eventId, COMPANY_ID);
        policy.addRule(new MinAgeRule(18));
        policyRepository.saveAndFlush(policy);

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByEventId(eventId).orElseThrow();

        assertFalse(loaded.validatePurchase(1, 16, false));
        assertTrue(loaded.validatePurchase(1, 18, false));
    }

    @Test
    @DisplayName("Given MaxTicketsRule, when quantity exceeds max, then rejected")
    void givenMaxTicketsRule_WhenQuantityExceedsMax_ThenRejected() {
        PurchasePolicy policy = new PurchasePolicy(6, "Max 2", eventId, COMPANY_ID);
        policy.addRule(new MaxTicketsRule(2));
        policyRepository.saveAndFlush(policy);

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByEventId(eventId).orElseThrow();

        assertFalse(loaded.validatePurchase(3, 20, false));
        assertTrue(loaded.validatePurchase(2, 20, false));
    }

    @Test
    @DisplayName("Given AndRule, when all conditions met, then approved")
    void givenAndRule_WhenAllConditionsMet_ThenApproved() {
        PurchasePolicy policy = new PurchasePolicy(7, "Age 18+ AND max 5", eventId, COMPANY_ID);
        policy.setRules(List.of(new MinAgeRule(18), new MaxTicketsRule(5)),
                PurchasePolicy.Operator.AND);
        policyRepository.saveAndFlush(policy);

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByEventId(eventId).orElseThrow();

        assertTrue(loaded.validatePurchase(3, 20, false));
        assertFalse(loaded.validatePurchase(6, 20, false));
        assertFalse(loaded.validatePurchase(1, 16, false));
    }

    @Test
    @DisplayName("Given OrRule, when one condition met, then approved")
    void givenOrRule_WhenOneConditionMet_ThenApproved() {
        PurchasePolicy policy = new PurchasePolicy(8, "Max 2 OR Min 100", eventId, COMPANY_ID);
        policy.setRules(List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);
        policyRepository.saveAndFlush(policy);

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByEventId(eventId).orElseThrow();

        assertTrue(loaded.validatePurchase(1, 20, false));
        assertTrue(loaded.validatePurchase(100, 20, false));
        assertFalse(loaded.validatePurchase(5, 20, false));
    }

    @Test
    @DisplayName("Given composite OR policy with mixed rules, when loaded, then OR is preserved")
     void givenMixedOrComposite_WhenLoaded_ThenOrPreserved() {
        // OR( MinAge(18), MaxTickets(2) )
        PurchasePolicy policy = new PurchasePolicy(15, "Mixed OR", eventId, COMPANY_ID);
        policy.setRules(List.of(new MinAgeRule(18), new MaxTicketsRule(2)),
                PurchasePolicy.Operator.OR);
        policyRepository.saveAndFlush(policy);

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByEventId(eventId).orElseThrow();

        assertTrue(loaded.validatePurchase(1, 20, false));
        assertTrue(loaded.validatePurchase(10, 20, false));
        assertTrue(loaded.validatePurchase(1, 16, false));
        assertFalse(loaded.validatePurchase(10, 16, false));
}
    // =========================================================================
    // Selling Policy — persistence round-trip
    // =========================================================================

    @Test
    @DisplayName("Given regular selling policy, when guest, then allowed")
    void givenRegularSellingPolicy_WhenGuest_ThenAllowed() {
        policyRepository.saveAndFlush(new SellingPolicy(
                9, "Regular", SellingPolicy.SellingType.REGULAR, eventId, COMPANY_ID));

        SellingPolicy loaded = policyRepository
                .findSellingPolicyByEventId(eventId).orElseThrow();

        assertTrue(loaded.isSelectionAllowed(false));
        assertTrue(loaded.isSelectionAllowed(true));
    }

    @Test
    @DisplayName("Given lottery selling policy, when guest, then rejected")
    void givenLotterySellingPolicy_WhenGuest_ThenRejected() {
        policyRepository.saveAndFlush(new SellingPolicy(
                10, "Lottery", SellingPolicy.SellingType.LOTTERY, eventId, COMPANY_ID));

        SellingPolicy loaded = policyRepository
                .findSellingPolicyByEventId(eventId).orElseThrow();

        assertFalse(loaded.isSelectionAllowed(false));
        assertTrue(loaded.isSelectionAllowed(true));
    }

    // =========================================================================
    // Company-scoped policies
    // =========================================================================

    @Test
    @DisplayName("Given company discount policy, when queried by companyId, then found")
    void givenCompanyDiscountPolicy_WhenQueriedByCompanyId_ThenFound() {
        policyRepository.saveAndFlush(
                new DiscountPolicy(11, "Company policy", null, COMPANY_ID));

        assertTrue(policyRepository
                .findDiscountPolicyByCompanyIdAndEventIdIsNull(COMPANY_ID).isPresent());
    }

    @Test
    @DisplayName("Given company purchase policy, when queried by companyId, then found")
    void givenCompanyPurchasePolicy_WhenQueriedByCompanyId_ThenFound() {
        policyRepository.saveAndFlush(
                new PurchasePolicy(12, "Company policy", null, COMPANY_ID));

        assertTrue(policyRepository
                .findPurchasePolicyByCompanyIdAndEventIdIsNull(COMPANY_ID).isPresent());
    }
}