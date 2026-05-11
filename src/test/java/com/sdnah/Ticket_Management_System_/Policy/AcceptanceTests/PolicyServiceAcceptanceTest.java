package com.sdnah.Ticket_Management_System_.Policy.AcceptanceTests;

import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PolicyService — Acceptance Tests")
@ExtendWith(MockitoExtension.class)
class PolicyServiceAcceptanceTest {

    @Mock
    private PolicyRepository policyRepo;

    private PolicyService policyService;

    private static final int  COMPANY_ID = 10;
    private static final UUID EVENT_ID   = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyRepo);
    }

    // =========================================================================
    // Selling Policy — Selection Permission
    // =========================================================================

    @Test
    @DisplayName("GivenRegularSellingPolicy_WhenGuestSelectsTickets_ThenSelectionApproved")
    void GivenRegularSellingPolicy_WhenGuestSelectsTickets_ThenSelectionApproved() {
        SellingPolicy sellingPolicy = new SellingPolicy(
                1, "Regular selling policy", SellingPolicy.SellingType.REGULAR, EVENT_ID);

        when(policyRepo.findSellingPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(sellingPolicy));

        assertTrue(policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false));
    }

    @Test
    @DisplayName("GivenLotterySellingPolicy_WhenGuestSelectsTickets_ThenSelectionRejected")
    void GivenLotterySellingPolicy_WhenGuestSelectsTickets_ThenSelectionRejected() {
        SellingPolicy sellingPolicy = new SellingPolicy(
                2, "Lottery selling policy", SellingPolicy.SellingType.LOTTERY, EVENT_ID);

        when(policyRepo.findSellingPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(sellingPolicy));

        assertFalse(policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false));
    }

    @Test
    @DisplayName("GivenLotterySellingPolicy_WhenMemberSelectsTickets_ThenSelectionApproved")
    void GivenLotterySellingPolicy_WhenMemberSelectsTickets_ThenSelectionApproved() {
        SellingPolicy sellingPolicy = new SellingPolicy(
                3, "Lottery selling policy", SellingPolicy.SellingType.LOTTERY, EVENT_ID);

        when(policyRepo.findSellingPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(sellingPolicy));

        assertTrue(policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, true));
    }

    // =========================================================================
    // Purchase Policy — Reserve Tickets
    // =========================================================================

    @Test
    @DisplayName("GivenValidPurchasePolicy_WhenGuestReservesTickets_ThenReservationApproved")
    void GivenValidPurchasePolicy_WhenGuestReservesTickets_ThenReservationApproved() {
        PurchasePolicy policy = new PurchasePolicy(4, "Default purchase policy", EVENT_ID);

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 2, 18));
    }

    @Test
    @DisplayName("GivenNoPurchasePolicy_WhenGuestReservesTickets_ThenDefaultApproved")
    void GivenNoPurchasePolicy_WhenGuestReservesTickets_ThenDefaultApproved() {
        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.empty());

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 2, 18));
    }

    @Test
    @DisplayName("GivenMinAgeRule_WhenBuyerIsTooYoung_ThenReservationRejected")
    void GivenMinAgeRule_WhenBuyerIsTooYoung_ThenReservationRejected() {
        PurchasePolicy policy = new PurchasePolicy(5, "18+ policy", EVENT_ID);
        policy.addRule(new MinAgeRule(18));

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertFalse(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 2, 16));
    }

    @Test
    @DisplayName("GivenMinAgeRule_WhenBuyerMeetsAge_ThenReservationApproved")
    void GivenMinAgeRule_WhenBuyerMeetsAge_ThenReservationApproved() {
        PurchasePolicy policy = new PurchasePolicy(6, "18+ policy", EVENT_ID);
        policy.addRule(new MinAgeRule(18));

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 2, 18));
    }

    @Test
    @DisplayName("GivenMaxTicketsRule_WhenQuantityExceedsMax_ThenReservationRejected")
    void GivenMaxTicketsRule_WhenQuantityExceedsMax_ThenReservationRejected() {
        PurchasePolicy policy = new PurchasePolicy(7, "Max 5 tickets", EVENT_ID);
        policy.addRule(new MaxTicketsRule(5));

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertFalse(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 6, 20));
    }

    @Test
    @DisplayName("GivenMinTicketsRule_WhenQuantityBelowMin_ThenReservationRejected")
    void GivenMinTicketsRule_WhenQuantityBelowMin_ThenReservationRejected() {
        PurchasePolicy policy = new PurchasePolicy(8, "Min 2 tickets", EVENT_ID);
        policy.addRule(new MinTicketsRule(2));

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertFalse(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 1, 20));
    }

    @Test
    @DisplayName("GivenAndRule_WhenAllConditionsMet_ThenReservationApproved")
    void GivenAndRule_WhenAllConditionsMet_ThenReservationApproved() {
        PurchasePolicy policy = new PurchasePolicy(9, "Age 18+ AND max 5", EVENT_ID);
        policy.setRules(List.of(new MinAgeRule(18), new MaxTicketsRule(5)),
                PurchasePolicy.Operator.AND);

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 3, 20));
    }

    @Test
    @DisplayName("GivenAndRule_WhenOneConditionFails_ThenReservationRejected")
    void GivenAndRule_WhenOneConditionFails_ThenReservationRejected() {
        PurchasePolicy policy = new PurchasePolicy(10, "Age 18+ AND max 5", EVENT_ID);
        policy.setRules(List.of(new MinAgeRule(18), new MaxTicketsRule(5)),
                PurchasePolicy.Operator.AND);

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        // age ok but quantity too high
        assertFalse(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 6, 20));
    }

    @Test
    @DisplayName("GivenOrRule_WhenOneConditionMet_ThenReservationApproved")
    void GivenOrRule_WhenOneConditionMet_ThenReservationApproved() {
        PurchasePolicy policy = new PurchasePolicy(11, "Max 2 OR Min 100", EVENT_ID);
        policy.setRules(List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        // qty=1 satisfies MaxTicketsRule(2)
        assertTrue(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 1, 20));
    }

    @Test
    @DisplayName("GivenOrRule_WhenNoConditionMet_ThenReservationRejected")
    void GivenOrRule_WhenNoConditionMet_ThenReservationRejected() {
        PurchasePolicy policy = new PurchasePolicy(12, "Max 2 OR Min 100", EVENT_ID);
        policy.setRules(List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        // qty=5 fails both
        assertFalse(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 5, 20));
    }

    // =========================================================================
    // Purchase Policy — Checkout
    // =========================================================================

    @Test
    @DisplayName("GivenValidPurchasePolicy_WhenCheckoutActiveOrder_ThenFinalPurchaseApproved")
    void GivenValidPurchasePolicy_WhenCheckoutActiveOrder_ThenFinalPurchaseApproved() {
        PurchasePolicy policy = new PurchasePolicy(13, "Checkout purchase policy", EVENT_ID);

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertTrue(policyService.validateFinalPurchaseConditions(COMPANY_ID, EVENT_ID, 2, 20));
    }

    // =========================================================================
    // Discount Policy — General Discounts
    // =========================================================================

    @Test
    @DisplayName("GivenPercentageDiscountPolicy_WhenCheckoutActiveOrder_ThenFinalPriceReduced")
    void GivenPercentageDiscountPolicy_WhenCheckoutActiveOrder_ThenFinalPriceReduced() {
        DiscountPolicy policy = new DiscountPolicy(14, "20% off", EVENT_ID);
        policy.addRule(new PercentageDiscountRule(20.0, "20% off"));

        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertEquals(80.0,
                policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 2),
                0.001);
    }

    @Test
    @DisplayName("GivenNoDiscountPolicy_WhenApplyGeneralDiscounts_ThenOriginalPriceReturned")
    void GivenNoDiscountPolicy_WhenApplyGeneralDiscounts_ThenOriginalPriceReturned() {
        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.empty());

        assertEquals(100.0,
                policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 2),
                0.001);
    }

    // =========================================================================
    // Discount Policy — Coupon
    // =========================================================================

    @Test
    @DisplayName("GivenCouponDiscountPolicy_WhenCheckoutWithValidCoupon_ThenCouponDiscountApplied")
    void GivenCouponDiscountPolicy_WhenCheckoutWithValidCoupon_ThenCouponDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(15, "Coupon discount", EVENT_ID);
        policy.addRule(new CouponDiscountRule(20.0, "SAVE20"));

        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertEquals(80.0,
                policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, "SAVE20"),
                0.001);
    }

    @Test
    @DisplayName("GivenCouponDiscountPolicy_WhenCheckoutWithInvalidCoupon_ThenOriginalPriceReturned")
    void GivenCouponDiscountPolicy_WhenCheckoutWithInvalidCoupon_ThenOriginalPriceReturned() {
        DiscountPolicy policy = new DiscountPolicy(16, "Coupon discount", EVENT_ID);
        policy.addRule(new CouponDiscountRule(20.0, "SAVE20"));

        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertEquals(100.0,
                policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, "WRONG"),
                0.001);
    }

    @Test
    @DisplayName("GivenCouponDiscountPolicy_WhenCheckoutWithNullCoupon_ThenOriginalPriceReturned")
    void GivenCouponDiscountPolicy_WhenCheckoutWithNullCoupon_ThenOriginalPriceReturned() {
        assertEquals(100.0,
                policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, null),
                0.001);
        verifyNoInteractions(policyRepo);
    }

    // =========================================================================
    // Discount Policy — Conditional Discount
    // =========================================================================

    @Test
    @DisplayName("GivenConditionalDiscount_WhenQuantityMeetsCondition_ThenConditionSatisfied")
    void GivenConditionalDiscount_WhenQuantityMeetsCondition_ThenConditionSatisfied() {
        DiscountPolicy policy = new DiscountPolicy(17, "Bulk discount", EVENT_ID);
        policy.addRule(new QuantityConditionalDiscountRule(3, 10.0));

        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertTrue(policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 3));
    }

    @Test
    @DisplayName("GivenConditionalDiscount_WhenQuantityBelowCondition_ThenConditionNotSatisfied")
    void GivenConditionalDiscount_WhenQuantityBelowCondition_ThenConditionNotSatisfied() {
        DiscountPolicy policy = new DiscountPolicy(18, "Bulk discount", EVENT_ID);
        policy.addRule(new QuantityConditionalDiscountRule(3, 10.0));

        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));

        assertFalse(policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 2));
    }

    // =========================================================================
    // UC II.4.3 — Add / Set / Clear Discount Rules
    // =========================================================================

    @Test
    @DisplayName("GivenDiscountPolicy_WhenAddDiscountRule_ThenRuleSaved")
    void GivenDiscountPolicy_WhenAddDiscountRule_ThenRuleSaved() {
        DiscountPolicy policy = new DiscountPolicy(19, "Empty policy", EVENT_ID);

        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);

        policyService.addDiscountRule(EVENT_ID, new PercentageDiscountRule(10.0, "10% off"));

        verify(policyRepo).savePolicy(policy);
        assertEquals(10.0, policy.computeDiscount(
                new com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount
                        .DiscountContext(1, null)), 0.001);
    }

    @Test
    @DisplayName("GivenNoDiscountPolicy_WhenAddDiscountRule_ThenExceptionThrown")
    void GivenNoDiscountPolicy_WhenAddDiscountRule_ThenExceptionThrown() {
        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                policyService.addDiscountRule(EVENT_ID,
                        new PercentageDiscountRule(10.0, "10% off")));
    }

    @Test
    @DisplayName("GivenDiscountPolicy_WhenSetDiscountRules_ThenRulesReplacedAndSaved")
    void GivenDiscountPolicy_WhenSetDiscountRules_ThenRulesReplacedAndSaved() {
        DiscountPolicy policy = new DiscountPolicy(20, "Old policy", EVENT_ID);
        policy.addRule(new PercentageDiscountRule(5.0, "5% off"));

        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);

        policyService.setDiscountRules(EVENT_ID,
                List.of(new PercentageDiscountRule(30.0, "30% off")), false);

        verify(policyRepo).savePolicy(policy);
        assertEquals(30.0, policy.computeDiscount(
                new com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount
                        .DiscountContext(1, null)), 0.001);
    }

    @Test
    @DisplayName("GivenDiscountPolicy_WhenClearDiscountRules_ThenNoDiscountApplied")
    void GivenDiscountPolicy_WhenClearDiscountRules_ThenNoDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(21, "Policy with rules", EVENT_ID);
        policy.addRule(new PercentageDiscountRule(20.0, "20% off"));

        when(policyRepo.findDiscountPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);

        policyService.clearDiscountRules(EVENT_ID);

        verify(policyRepo).savePolicy(policy);
        assertEquals(0.0, policy.computeDiscount(
                new com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount
                        .DiscountContext(1, null)), 0.001);
    }

    // =========================================================================
    // UC II.4.3 — Add / Set / Clear Purchase Rules
    // =========================================================================

    @Test
    @DisplayName("GivenPurchasePolicy_WhenAddPurchaseRule_ThenRuleSaved")
    void GivenPurchasePolicy_WhenAddPurchaseRule_ThenRuleSaved() {
        PurchasePolicy policy = new PurchasePolicy(22, "Empty policy", EVENT_ID);

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);

        policyService.addPurchaseRule(EVENT_ID, new MinAgeRule(18));

        verify(policyRepo).savePolicy(policy);
        assertFalse(policy.validatePurchase(1, 16, false)); // age 16 fails
        assertTrue(policy.validatePurchase(1, 18, false));  // age 18 passes
    }

    @Test
    @DisplayName("GivenNoPurchasePolicy_WhenAddPurchaseRule_ThenExceptionThrown")
    void GivenNoPurchasePolicy_WhenAddPurchaseRule_ThenExceptionThrown() {
        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                policyService.addPurchaseRule(EVENT_ID, new MinAgeRule(18)));
    }

    @Test
    @DisplayName("GivenPurchasePolicy_WhenSetPurchaseRulesAndOr_ThenRulesReplacedAndSaved")
    void GivenPurchasePolicy_WhenSetPurchaseRulesAndOr_ThenRulesReplacedAndSaved() {
        PurchasePolicy policy = new PurchasePolicy(23, "Old policy", EVENT_ID);

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);

        // Max 2 OR Min 100
        policyService.setPurchaseRules(EVENT_ID,
                List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);

        verify(policyRepo).savePolicy(policy);
        assertTrue(policy.validatePurchase(1, 20, false));   // qty=1 passes MaxTickets
        assertTrue(policy.validatePurchase(100, 20, false)); // qty=100 passes MinTickets
        assertFalse(policy.validatePurchase(5, 20, false));  // qty=5 fails both
    }

    @Test
    @DisplayName("GivenPurchasePolicy_WhenClearPurchaseRules_ThenAllPurchasesAllowed")
    void GivenPurchasePolicy_WhenClearPurchaseRules_ThenAllPurchasesAllowed() {
        PurchasePolicy policy = new PurchasePolicy(24, "Policy with rules", EVENT_ID);
        policy.addRule(new MinAgeRule(18));

        when(policyRepo.findPurchasePolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);

        policyService.clearPurchaseRules(EVENT_ID);

        verify(policyRepo).savePolicy(policy);
        assertTrue(policy.validatePurchase(1, 16, false)); // no rules = allowed
    }

    // =========================================================================
    // Concurrency
    // =========================================================================

    @Test
    @Timeout(5)
    @DisplayName("GivenManyConcurrentUsers_WhenCheckingLotterySelection_ThenAllGuestRequestsRejected")
    void GivenManyConcurrentUsers_WhenCheckingLotterySelection_ThenAllGuestRequestsRejected()
            throws Exception {
        SellingPolicy sellingPolicy = new SellingPolicy(
                25, "Lottery selling policy", SellingPolicy.SellingType.LOTTERY, EVENT_ID);

        when(policyRepo.findSellingPolicyByEventId2(EVENT_ID))
                .thenReturn(Optional.of(sellingPolicy));

        int numberOfUsers = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch readyLatch = new CountDownLatch(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(numberOfUsers);
        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < numberOfUsers; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                results.add(policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false));
                doneLatch.countDown();
                return null;
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(numberOfUsers, results.size());
        assertTrue(results.stream().allMatch(r -> !r));
    }
}