package com.sdnah.Ticket_Management_System_.Policy.UnitTests;
import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PolicyService — Application Layer Unit Tests")
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest 
{
    @Mock
    private IPolicyRepo policyRepo;

    private PolicyService policyService;

    private static final int COMPANY_ID = 10;
    private static final int EVENT_ID = 20;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyRepo);
    }

    // =========================================================================
    // UC II.2.5 — Select Tickets for Event
    // =========================================================================

    @Test
    void GivenRegularSellingPolicy_WhenCheckSelectionPermission_ThenGuestAllowed() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular selling",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);

        assertTrue(result);
    }

    @Test
    void GivenLotterySellingPolicy_WhenGuestChecksSelectionPermission_ThenReturnFalse() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Lottery selling",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);

        assertFalse(result);
    }

    @Test
    void GivenLotterySellingPolicy_WhenMemberChecksSelectionPermission_ThenReturnTrue() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Lottery selling",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, true);

        assertTrue(result);
    }

    @Test
    void GivenNoSellingPolicy_WhenCheckSelectionPermission_ThenDefaultAllow() {
        when(policyRepo.findAll()).thenReturn(List.of());

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);

        assertTrue(result);
    }

    // =========================================================================
    // UC II.2.4 — Reserve Tickets in Active Order
    // =========================================================================

    @Test
    void GivenNoPurchasePolicy_WhenValidateReservation_ThenDefaultAllow() {
        when(policyRepo.findAll()).thenReturn(List.of());

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 3, 18);

        assertTrue(result);
    }

    @Test
    void GivenPurchasePolicy_WhenQuantityPositiveAndAgeValid_ThenReservationApproved() {
        PurchasePolicy policy = new PurchasePolicy(
                2,
                "Default purchase",
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 1, 18);

        assertTrue(result);
    }

    @Test
    void GivenPurchasePolicy_WhenQuantityZero_ThenReservationRejected() {
        PurchasePolicy policy = new PurchasePolicy(
                2,
                "Default purchase",
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 0, 18);

        assertFalse(result);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order
    // =========================================================================

    @Test
    void GivenNoPurchasePolicy_WhenValidateFinalPurchase_ThenDefaultAllow() {
        when(policyRepo.findAll()).thenReturn(List.of());

        boolean result = policyService.validateFinalPurchaseConditions(COMPANY_ID, EVENT_ID, 2, 18);

        assertTrue(result);
    }

    @Test
    void GivenPurchasePolicy_WhenFinalQuantityZero_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(
                3,
                "Checkout purchase",
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        boolean result = policyService.validateFinalPurchaseConditions(COMPANY_ID, EVENT_ID, 0, 18);

        assertFalse(result);
    }

    @Test
    void GivenDiscountPolicyWithPercentage_WhenApplyGeneralDiscounts_ThenReturnDiscountedPrice() {
        DiscountPolicy policy = new DiscountPolicy(
                4,
                "Discount policy",
                EVENT_ID,
                COMPANY_ID
        );
        policy.addDiscountRule(new DiscountPolicy.PercentageDiscount(20));

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        double result = policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 2);

        assertEquals(80.0, result, 0.001);
    }

    @Test
    void GivenNoDiscountPolicy_WhenApplyGeneralDiscounts_ThenReturnBasePrice() {
        when(policyRepo.findAll()).thenReturn(List.of());

        double result = policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 2);

        assertEquals(100.0, result, 0.001);
    }

    @Test
    void GivenCouponDiscountAndCorrectCoupon_WhenCalculateCouponDiscount_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(
                5,
                "Coupon discount",
                EVENT_ID,
                COMPANY_ID
        );
        policy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        double result = policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, "SAVE20");

        assertEquals(80.0, result, 0.001);
    }

    @Test
    void GivenCouponDiscountAndWrongCoupon_WhenCalculateCouponDiscount_ThenOriginalPriceReturned() {
        DiscountPolicy policy = new DiscountPolicy(
                5,
                "Coupon discount",
                EVENT_ID,
                COMPANY_ID
        );
        policy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        double result = policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, "WRONG");

        assertEquals(100.0, result, 0.001);
    }

    @Test
    void GivenEmptyCoupon_WhenCalculateCouponDiscount_ThenRepositoryNotUsedAndOriginalReturned() {
        double result = policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, "");

        assertEquals(100.0, result, 0.001);
        verify(policyRepo, never()).findAll();
    }

    @Test
    void GivenConditionalDiscountAndQuantityEnough_WhenCheckConditionalDiscount_ThenReturnTrue() {
        DiscountPolicy policy = new DiscountPolicy(
                6,
                "Conditional discount",
                EVENT_ID,
                COMPANY_ID
        );
        policy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 10));

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        boolean result = policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 3);

        assertTrue(result);
    }

    @Test
    void GivenConditionalDiscountAndQuantityTooLow_WhenCheckConditionalDiscount_ThenReturnFalse() {
        DiscountPolicy policy = new DiscountPolicy(
                6,
                "Conditional discount",
                EVENT_ID,
                COMPANY_ID
        );
        policy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 10));

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        boolean result = policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 2);

        assertFalse(result);
    }

    // =========================================================================
    // Fallback — Event Policy before Company Default Policy
    // Requires constructors with Integer eventId.
    // =========================================================================

    @Test
    void GivenEventPolicyAndCompanyDefaultPolicy_WhenApplyDiscount_ThenEventPolicyHasPriority() {
        DiscountPolicy companyPolicy = new DiscountPolicy(
                7,
                "Company default discount",
                null,
                COMPANY_ID
        );
        companyPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        DiscountPolicy eventPolicy = new DiscountPolicy(
                8,
                "Event specific discount",
                EVENT_ID,
                COMPANY_ID
        );
        eventPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(30));

        when(policyRepo.findAll()).thenReturn(List.of(companyPolicy, eventPolicy));

        double result = policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 1);

        assertEquals(70.0, result, 0.001);
    }

    @Test
    void GivenOnlyCompanyDefaultPolicy_WhenApplyDiscount_ThenCompanyPolicyUsed() {
        DiscountPolicy companyPolicy = new DiscountPolicy(
                7,
                "Company default discount",
                null,
                COMPANY_ID
        );
        companyPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        when(policyRepo.findAll()).thenReturn(List.of(companyPolicy));

        double result = policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 1);

        assertEquals(90.0, result, 0.001);
    }

    // =========================================================================
    // UC II.4.3 — Not implemented in Version 1
    // =========================================================================

    @Test
    void GivenVersionOne_WhenChangePurchasePolicy_ThenThrowUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> policyService.changePurchasePolicy());
    }

    @Test
    void GivenVersionOne_WhenChangeDiscountPolicy_ThenThrowUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> policyService.changeDiscountPolicy());
    }

    // =========================================================================
    // Concurrency Tests — ExecutorService + CountDownLatch
    // =========================================================================

    @Test
    @Timeout(5)
    void GivenManyThreads_WhenCheckSelectionPermissionConcurrently_ThenAllResultsConsistent() throws Exception {
        SellingPolicy policy = new SellingPolicy(
                9,
                "Lottery policy",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                start.await();

                boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false);
                results.add(result);

                done.countDown();
                return null;
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        assertEquals(threads, results.size());
        assertTrue(results.stream().allMatch(result -> result == false));
    }
}