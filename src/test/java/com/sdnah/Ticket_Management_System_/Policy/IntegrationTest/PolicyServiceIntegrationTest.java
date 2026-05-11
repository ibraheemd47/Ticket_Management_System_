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

import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository;

@SpringBootTest
@ActiveProfiles("test")
class PolicyServiceIntegrationTest {

    private static final int COMPANY_ID = 10;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private PolicyRepository policyRepository;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
        eventId = UUID.randomUUID();
    }

    // =========================================================================
    // Selling Policy — Selection Permission
    // =========================================================================

    @Test
    @DisplayName("Given regular selling policy, when guest selects tickets, then selection is approved")
    void givenRegularSellingPolicy_WhenGuestSelectsTickets_ThenSelectionIsApproved() {
        policyRepository.saveAndFlush(new SellingPolicy(
                1, "Regular policy", SellingPolicy.SellingType.REGULAR, eventId));

        assertTrue(policyService.checkSelectionPermission(COMPANY_ID, eventId, false));
    }

    @Test
    @DisplayName("Given lottery selling policy, when guest selects tickets, then selection is rejected")
    void givenLotterySellingPolicy_WhenGuestSelectsTickets_ThenSelectionIsRejected() {
        policyRepository.saveAndFlush(new SellingPolicy(
                2, "Lottery policy", SellingPolicy.SellingType.LOTTERY, eventId));

        assertFalse(policyService.checkSelectionPermission(COMPANY_ID, eventId, false));
    }

    @Test
    @DisplayName("Given lottery selling policy, when member selects tickets, then selection is approved")
    void givenLotterySellingPolicy_WhenMemberSelectsTickets_ThenSelectionIsApproved() {
        policyRepository.saveAndFlush(new SellingPolicy(
                3, "Lottery policy", SellingPolicy.SellingType.LOTTERY, eventId));

        assertTrue(policyService.checkSelectionPermission(COMPANY_ID, eventId, true));
    }

    @Test
    @DisplayName("Given no selling policy, when checking selection, then default approves")
    void givenNoSellingPolicy_WhenCheckingSelection_ThenDefaultApproves() {
        assertTrue(policyService.checkSelectionPermission(COMPANY_ID, eventId, false));
    }

    // =========================================================================
    // Purchase Policy — Reserve Tickets
    // =========================================================================

    @Test
    @DisplayName("Given purchase policy with no rules, when validating reservation, then approved")
    void givenPurchasePolicyNoRules_WhenValidatingReservation_ThenApproved() {
        policyRepository.saveAndFlush(new PurchasePolicy(4, "Standard purchase", eventId));

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, eventId, 2, 25));
    }

    @Test
    @DisplayName("Given MaxTicketsRule, when quantity exceeds max, then rejected")
    void givenMaxTicketsRule_WhenQuantityExceedsMax_ThenRejected() {
        PurchasePolicy policy = new PurchasePolicy(5, "Max 2 tickets", eventId);
        policy.addRule(new MaxTicketsRule(2));
        policyRepository.saveAndFlush(policy);

        assertFalse(policyService.validateReservationRequest(COMPANY_ID, eventId, 3, 25));
    }

    @Test
    @DisplayName("Given MaxTicketsRule, when quantity within max, then approved")
    void givenMaxTicketsRule_WhenQuantityWithinMax_ThenApproved() {
        PurchasePolicy policy = new PurchasePolicy(6, "Max 5 tickets", eventId);
        policy.addRule(new MaxTicketsRule(5));
        policyRepository.saveAndFlush(policy);

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, eventId, 3, 25));
    }

    @Test
    @DisplayName("Given MinAgeRule, when buyer is too young, then rejected")
    void givenMinAgeRule_WhenBuyerTooYoung_ThenRejected() {
        PurchasePolicy policy = new PurchasePolicy(7, "18+ policy", eventId);
        policy.addRule(new MinAgeRule(18));
        policyRepository.saveAndFlush(policy);

        assertFalse(policyService.validateReservationRequest(COMPANY_ID, eventId, 1, 16));
    }

    @Test
    @DisplayName("Given MinAgeRule, when buyer meets minimum age, then approved")
    void givenMinAgeRule_WhenBuyerMeetsAge_ThenApproved() {
        PurchasePolicy policy = new PurchasePolicy(8, "18+ policy", eventId);
        policy.addRule(new MinAgeRule(18));
        policyRepository.saveAndFlush(policy);

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, eventId, 1, 18));
    }

    @Test
    @DisplayName("Given no purchase policy, when validating reservation, then default approves")
    void givenNoPurchasePolicy_WhenValidatingReservation_ThenDefaultApproves() {
        assertTrue(policyService.validateReservationRequest(COMPANY_ID, eventId, 2, 25));
    }

    @Test
    @DisplayName("Given AndRule with age and max tickets, when all conditions met, then approved")
    void givenAndRule_WhenAllConditionsMet_ThenApproved() {
        PurchasePolicy policy = new PurchasePolicy(9, "Age 18+ AND max 5", eventId);
        policy.setRules(List.of(new MinAgeRule(18), new MaxTicketsRule(5)),
                PurchasePolicy.Operator.AND);
        policyRepository.saveAndFlush(policy);

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, eventId, 3, 20));
    }

    @Test
    @DisplayName("Given AndRule with age and max tickets, when one condition fails, then rejected")
    void givenAndRule_WhenOneConditionFails_ThenRejected() {
        PurchasePolicy policy = new PurchasePolicy(10, "Age 18+ AND max 5", eventId);
        policy.setRules(List.of(new MinAgeRule(18), new MaxTicketsRule(5)),
                PurchasePolicy.Operator.AND);
        policyRepository.saveAndFlush(policy);

        assertFalse(policyService.validateReservationRequest(COMPANY_ID, eventId, 6, 20));
    }

    @Test
    @DisplayName("Given OrRule, when one condition met, then approved")
    void givenOrRule_WhenOneConditionMet_ThenApproved() {
        PurchasePolicy policy = new PurchasePolicy(11, "Max 2 OR Min 100", eventId);
        policy.setRules(List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);
        policyRepository.saveAndFlush(policy);

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, eventId, 1, 20));
    }

    @Test
    @DisplayName("Given OrRule, when no condition met, then rejected")
    void givenOrRule_WhenNoConditionMet_ThenRejected() {
        PurchasePolicy policy = new PurchasePolicy(12, "Max 2 OR Min 100", eventId);
        policy.setRules(List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);
        policyRepository.saveAndFlush(policy);

        assertFalse(policyService.validateReservationRequest(COMPANY_ID, eventId, 5, 20));
    }

    // =========================================================================
    // Purchase Policy — Checkout
    // =========================================================================

    @Test
    @DisplayName("Given purchase policy, when final purchase conditions valid, then approved")
    void givenPurchasePolicy_WhenFinalPurchaseConditionsValid_ThenApproved() {
        policyRepository.saveAndFlush(new PurchasePolicy(13, "Final purchase policy", eventId));

        assertTrue(policyService.validateFinalPurchaseConditions(COMPANY_ID, eventId, 2, 25));
    }

    // =========================================================================
    // Discount Policy — General Discounts
    // =========================================================================

    @Test
    @DisplayName("Given no discount policy, when applying general discounts, then base price unchanged")
    void givenNoDiscountPolicy_WhenApplyingGeneralDiscounts_ThenBasePriceUnchanged() {
        assertEquals(100.0,
                policyService.applyGeneralDiscounts(COMPANY_ID, eventId, 100.0, 1));
    }

    @Test
    @DisplayName("Given PercentageDiscountRule 10%, when applying general discounts, then price reduced by 10%")
    void givenPercentageDiscountRule_WhenApplyingGeneralDiscounts_ThenPriceReduced() {
        DiscountPolicy policy = new DiscountPolicy(14, "10% off", eventId);
        policy.addRule(new PercentageDiscountRule(10.0, "10% off"));
        policyRepository.saveAndFlush(policy);

        assertEquals(90.0,
                policyService.applyGeneralDiscounts(COMPANY_ID, eventId, 100.0, 1),
                0.001);
    }

    // =========================================================================
    // Discount Policy — Coupon
    // =========================================================================

    @Test
    @DisplayName("Given coupon discount, when applying matching coupon, then discount is applied")
    void givenCouponDiscount_WhenApplyingMatchingCoupon_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(15, "Coupon SAVE10", eventId);
        policy.addRule(new CouponDiscountRule(10.0, "SAVE10"));
        policyRepository.saveAndFlush(policy);

        assertEquals(90.0,
                policyService.calculateCouponDiscount(COMPANY_ID, eventId, 100.0, 1, "SAVE10"),
                0.001);
    }

    @Test
    @DisplayName("Given coupon discount, when wrong coupon applied, then price unchanged")
    void givenCouponDiscount_WhenWrongCoupon_ThenPriceUnchanged() {
        DiscountPolicy policy = new DiscountPolicy(16, "Coupon SAVE10", eventId);
        policy.addRule(new CouponDiscountRule(10.0, "SAVE10"));
        policyRepository.saveAndFlush(policy);

        assertEquals(100.0,
                policyService.calculateCouponDiscount(COMPANY_ID, eventId, 100.0, 1, "WRONG"),
                0.001);
    }

    @Test
    @DisplayName("Given blank coupon, when calculating coupon discount, then price unchanged")
    void givenBlankCoupon_WhenCalculatingCouponDiscount_ThenPriceUnchanged() {
        assertEquals(100.0,
                policyService.calculateCouponDiscount(COMPANY_ID, eventId, 100.0, 1, "   "));
    }

    // =========================================================================
    // Discount Policy — Conditional Discount
    // =========================================================================

    @Test
    @DisplayName("Given conditional discount, when quantity meets threshold, then satisfied")
    void givenConditionalDiscount_WhenQuantityMeetsThreshold_ThenSatisfied() {
        DiscountPolicy policy = new DiscountPolicy(17, "Buy 3 get discount", eventId);
        policy.addRule(new QuantityConditionalDiscountRule(3, 20.0));
        policyRepository.saveAndFlush(policy);

        assertTrue(policyService.isConditionalDiscountSatisfied(COMPANY_ID, eventId, 3));
    }

    @Test
    @DisplayName("Given conditional discount, when quantity below threshold, then not satisfied")
    void givenConditionalDiscount_WhenQuantityBelowThreshold_ThenNotSatisfied() {
        DiscountPolicy policy = new DiscountPolicy(18, "Buy 3 get discount", eventId);
        policy.addRule(new QuantityConditionalDiscountRule(3, 20.0));
        policyRepository.saveAndFlush(policy);

        assertFalse(policyService.isConditionalDiscountSatisfied(COMPANY_ID, eventId, 2));
    }

    @Test
    @DisplayName("Given no discount policy, when checking conditional discount, then false returned")
    void givenNoDiscountPolicy_WhenCheckingConditionalDiscount_ThenFalseReturned() {
        assertFalse(policyService.isConditionalDiscountSatisfied(COMPANY_ID, eventId, 3));
    }

    // =========================================================================
    // UC II.4.3 — Add / Set / Clear Discount Rules (persisted)
    // =========================================================================

    @Test
    @DisplayName("Given discount policy, when addDiscountRule, then rule persisted and discount applied")
    void givenDiscountPolicy_WhenAddDiscountRule_ThenRulePersistedAndApplied() {
        DiscountPolicy policy = new DiscountPolicy(19, "Empty policy", eventId);
        policyRepository.saveAndFlush(policy);

        policyService.addDiscountRule(eventId, new PercentageDiscountRule(20.0, "20% off"));

        assertEquals(80.0,
                policyService.applyGeneralDiscounts(COMPANY_ID, eventId, 100.0, 1),
                0.001);
    }

    @Test
    @DisplayName("Given discount policy, when clearDiscountRules, then no discount applied")
    void givenDiscountPolicy_WhenClearDiscountRules_ThenNoDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(20, "Policy with rule", eventId);
        policy.addRule(new PercentageDiscountRule(20.0, "20% off"));
        policyRepository.saveAndFlush(policy);

        policyService.clearDiscountRules(eventId);

        assertEquals(100.0,
                policyService.applyGeneralDiscounts(COMPANY_ID, eventId, 100.0, 1),
                0.001);
    }

    // =========================================================================
    // UC II.4.3 — Add / Set / Clear Purchase Rules (persisted)
    // =========================================================================

    @Test
    @DisplayName("Given purchase policy, when addPurchaseRule, then rule persisted and validated")
    void givenPurchasePolicy_WhenAddPurchaseRule_ThenRulePersistedAndValidated() {
        PurchasePolicy policy = new PurchasePolicy(21, "Empty policy", eventId);
        policyRepository.saveAndFlush(policy);

        policyService.addPurchaseRule(eventId, new MinAgeRule(18));

        assertFalse(policyService.validateReservationRequest(COMPANY_ID, eventId, 1, 16));
        assertTrue(policyService.validateReservationRequest(COMPANY_ID, eventId, 1, 18));
    }

    @Test
    @DisplayName("Given purchase policy with rules, when clearPurchaseRules, then all purchases allowed")
    void givenPurchasePolicyWithRules_WhenClearPurchaseRules_ThenAllAllowed() {
        PurchasePolicy policy = new PurchasePolicy(22, "Policy with rule", eventId);
        policy.addRule(new MinAgeRule(18));
        policyRepository.saveAndFlush(policy);

        policyService.clearPurchaseRules(eventId);

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, eventId, 1, 16));
    }
}