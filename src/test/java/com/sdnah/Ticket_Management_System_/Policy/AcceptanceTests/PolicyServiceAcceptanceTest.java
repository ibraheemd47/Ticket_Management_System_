package com.sdnah.Ticket_Management_System_.Policy.AcceptanceTests;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.*;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private static final int COMPANY_ID = 10;
    private static final UUID EVENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyRepo);
    }

    @Test
    @DisplayName("GivenRegularSellingPolicy_WhenGuestSelectsTickets_ThenSelectionApproved")
    void GivenRegularSellingPolicy_WhenGuestSelectsTickets_ThenSelectionApproved() {
        SellingPolicy sellingPolicy = new SellingPolicy(
                1,
                "Regular selling policy",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID
        );

        when(policyRepo.findSellingPolicyByEventId(EVENT_ID)).thenReturn(sellingPolicy);

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);

        assertTrue(result);
    }

    @Test
    @DisplayName("GivenLotterySellingPolicy_WhenGuestSelectsTickets_ThenSelectionRejected")
    void GivenLotterySellingPolicy_WhenGuestSelectsTickets_ThenSelectionRejected() {
        SellingPolicy sellingPolicy = new SellingPolicy(
                2,
                "Lottery selling policy",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID
        );

        when(policyRepo.findSellingPolicyByEventId(EVENT_ID)).thenReturn(sellingPolicy);

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);

        assertFalse(result);
    }

    @Test
    @DisplayName("GivenLotterySellingPolicy_WhenMemberSelectsTickets_ThenSelectionApproved")
    void GivenLotterySellingPolicy_WhenMemberSelectsTickets_ThenSelectionApproved() {
        SellingPolicy sellingPolicy = new SellingPolicy(
                3,
                "Lottery selling policy",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID
        );

        when(policyRepo.findSellingPolicyByEventId(EVENT_ID)).thenReturn(sellingPolicy);

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, true);

        assertTrue(result);
    }

    @Test
    @DisplayName("GivenValidPurchasePolicy_WhenGuestReservesTickets_ThenReservationApproved")
    void GivenValidPurchasePolicy_WhenGuestReservesTickets_ThenReservationApproved() {
        PurchasePolicy purchasePolicy = new PurchasePolicy(
                4,
                "Default purchase policy",
                EVENT_ID
        );

        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(purchasePolicy);

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 2, 18);

        assertTrue(result);
    }

    @Test
    @DisplayName("GivenPurchasePolicy_WhenGuestReservesZeroTickets_ThenReservationRejected")
    void GivenPurchasePolicy_WhenGuestReservesZeroTickets_ThenReservationRejected() {
        PurchasePolicy purchasePolicy = new PurchasePolicy(
                5,
                "Default purchase policy",
                EVENT_ID
        );

        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(purchasePolicy);

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 0, 18);

        assertFalse(result);
    }

    @Test
    @DisplayName("GivenNoPurchasePolicy_WhenGuestReservesTickets_ThenDefaultReservationApproved")
    void GivenNoPurchasePolicy_WhenGuestReservesTickets_ThenDefaultReservationApproved() {
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(null);

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 2, 18);

        assertTrue(result);
    }

    @Test
    @DisplayName("GivenValidPurchasePolicy_WhenCheckoutActiveOrder_ThenFinalPurchaseApproved")
    void GivenValidPurchasePolicy_WhenCheckoutActiveOrder_ThenFinalPurchaseApproved() {
        PurchasePolicy purchasePolicy = new PurchasePolicy(
                6,
                "Checkout purchase policy",
                EVENT_ID
        );

        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(purchasePolicy);

        boolean result = policyService.validateFinalPurchaseConditions(COMPANY_ID, EVENT_ID, 2);

        assertTrue(result);
    }

    @Test
    @DisplayName("GivenPurchasePolicy_WhenCheckoutWithZeroTickets_ThenFinalPurchaseRejected")
    void GivenPurchasePolicy_WhenCheckoutWithZeroTickets_ThenFinalPurchaseRejected() {
        PurchasePolicy purchasePolicy = new PurchasePolicy(
                7,
                "Checkout purchase policy",
                EVENT_ID
        );

        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(purchasePolicy);

        boolean result = policyService.validateFinalPurchaseConditions(COMPANY_ID, EVENT_ID, 0);

        assertFalse(result);
    }

    @Test
    @DisplayName("GivenPercentageDiscountPolicy_WhenCheckoutActiveOrder_ThenFinalPriceReduced")
    void GivenPercentageDiscountPolicy_WhenCheckoutActiveOrder_ThenFinalPriceReduced() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                8,
                "General discount policy",
                EVENT_ID
        );
        discountPolicy.addDiscount(new DiscountPolicy.PercentageDiscount(20));

        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(discountPolicy);

        double result = policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 2);

        assertEquals(80.0, result, 0.001);
    }

    @Test
    @DisplayName("GivenCouponDiscountPolicy_WhenCheckoutWithValidCoupon_ThenCouponDiscountApplied")
    void GivenCouponDiscountPolicy_WhenCheckoutWithValidCoupon_ThenCouponDiscountApplied() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                9,
                "Coupon discount policy",
                EVENT_ID
        );
        discountPolicy.addDiscount(new DiscountPolicy.CouponDiscount("SAVE20", 20));

        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(discountPolicy);

        double result = policyService.calculateCouponDiscount(
                COMPANY_ID,
                EVENT_ID,
                100.0,
                2,
                "SAVE20"
        );

        assertEquals(80.0, result, 0.001);
    }

    @Test
    @DisplayName("GivenCouponDiscountPolicy_WhenCheckoutWithInvalidCoupon_ThenOriginalPriceReturned")
    void GivenCouponDiscountPolicy_WhenCheckoutWithInvalidCoupon_ThenOriginalPriceReturned() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                10,
                "Coupon discount policy",
                EVENT_ID
        );
        discountPolicy.addDiscount(new DiscountPolicy.CouponDiscount("SAVE20", 20));

        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(discountPolicy);

        double result = policyService.calculateCouponDiscount(
                COMPANY_ID,
                EVENT_ID,
                100.0,
                2,
                "WRONG"
        );

        assertEquals(100.0, result, 0.001);
    }

    @Test
    @DisplayName("GivenConditionalDiscountPolicy_WhenQuantityMeetsCondition_ThenConditionSatisfied")
    void GivenConditionalDiscountPolicy_WhenQuantityMeetsCondition_ThenConditionSatisfied() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                11,
                "Conditional discount policy",
                EVENT_ID
        );
        discountPolicy.addDiscount(new DiscountPolicy.ConditionalDiscount(3, 10));

        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(discountPolicy);

        boolean result = policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 3);

        assertTrue(result);
    }

    @Test
    @DisplayName("GivenConditionalDiscountPolicy_WhenQuantityDoesNotMeetCondition_ThenConditionNotSatisfied")
    void GivenConditionalDiscountPolicy_WhenQuantityDoesNotMeetCondition_ThenConditionNotSatisfied() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                12,
                "Conditional discount policy",
                EVENT_ID
        );
        discountPolicy.addDiscount(new DiscountPolicy.ConditionalDiscount(3, 10));

        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(discountPolicy);

        boolean result = policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 2);

        assertFalse(result);
    }

    @Test
    @DisplayName("GivenVersionOne_WhenChangePurchasePolicyRequested_ThenUnsupportedOperationThrown")
    void GivenVersionOne_WhenChangePurchasePolicyRequested_ThenUnsupportedOperationThrown() {
        assertThrows(UnsupportedOperationException.class, () ->
                policyService.changePurchasePolicy()
        );
    }

    @Test
    @DisplayName("GivenVersionOne_WhenChangeDiscountPolicyRequested_ThenUnsupportedOperationThrown")
    void GivenVersionOne_WhenChangeDiscountPolicyRequested_ThenUnsupportedOperationThrown() {
        assertThrows(UnsupportedOperationException.class, () ->
                policyService.changeDiscountPolicy()
        );
    }

    @Test
    @Timeout(5)
    @DisplayName("GivenManyConcurrentUsers_WhenCheckingLotterySelection_ThenAllGuestRequestsRejectedConsistently")
    void GivenManyConcurrentUsers_WhenCheckingLotterySelection_ThenAllGuestRequestsRejectedConsistently() throws Exception {
        SellingPolicy sellingPolicy = new SellingPolicy(
                16,
                "Lottery selling policy",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID
        );

        when(policyRepo.findSellingPolicyByEventId(EVENT_ID)).thenReturn(sellingPolicy);

        int numberOfUsers = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers);

        CountDownLatch readyLatch = new CountDownLatch(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfUsers);

        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < numberOfUsers; i++) {
            executorService.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);
                results.add(result);

                doneLatch.countDown();
                return null;
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        executorService.shutdown();

        assertEquals(numberOfUsers, results.size());
        assertTrue(results.stream().allMatch(result -> !result));
    }
}