package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PolicyService — Application Layer Unit Tests")
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

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
    // Selling Policy — checkSelectionPermission
    // =========================================================================

    @Test
    void GivenRegularSellingPolicy_WhenCheckSelectionPermission_ThenGuestAllowed() {
        SellingPolicy policy = new SellingPolicy(1, "Regular selling",
                SellingPolicy.SellingType.REGULAR, EVENT_ID);
        when(policyRepo.findSellingPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertTrue(policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false));
    }

    @Test
    void GivenLotterySellingPolicy_WhenGuestChecksSelectionPermission_ThenReturnFalse() {
        SellingPolicy policy = new SellingPolicy(1, "Lottery selling",
                SellingPolicy.SellingType.LOTTERY, EVENT_ID);
        when(policyRepo.findSellingPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertFalse(policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false));
    }

    @Test
    void GivenLotterySellingPolicy_WhenMemberChecksSelectionPermission_ThenReturnTrue() {
        SellingPolicy policy = new SellingPolicy(1, "Lottery selling",
                SellingPolicy.SellingType.LOTTERY, EVENT_ID);
        when(policyRepo.findSellingPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertTrue(policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, true));
    }

    @Test
    void GivenNoSellingPolicy_WhenCheckSelectionPermission_ThenDefaultAllow() {
        when(policyRepo.findSellingPolicyByEventId(EVENT_ID)).thenReturn(Optional.empty());

        assertTrue(policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false));
    }

    // =========================================================================
    // Purchase Policy — validateReservationRequest
    // =========================================================================

    @Test
    void GivenNoPurchasePolicy_WhenValidateReservation_ThenDefaultAllow() {
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.empty());

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 3, 18));
    }

    @Test
    void GivenPurchasePolicy_WhenQuantityPositive_ThenReservationApproved() {
        PurchasePolicy policy = new PurchasePolicy(2, "Default purchase", EVENT_ID);
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertTrue(policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 1, 18));
    }

    // =========================================================================
    // Purchase Policy — validateFinalPurchaseConditions
    // =========================================================================

    @Test
    void GivenNoPurchasePolicy_WhenValidateFinalPurchase_ThenDefaultAllow() {
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.empty());

        assertTrue(policyService.validateFinalPurchaseConditions(COMPANY_ID, EVENT_ID, 2, 18));
    }

    @Test
    void GivenPurchasePolicy_WhenFinalPurchaseValid_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(3, "Checkout purchase", EVENT_ID);
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertTrue(policyService.validateFinalPurchaseConditions(COMPANY_ID, EVENT_ID, 2, 18));
    }

    // =========================================================================
    // Discount Policy — applyGeneralDiscounts
    // =========================================================================

    @Test
    void GivenDiscountPolicyWithPercentage_WhenApplyGeneralDiscounts_ThenReturnDiscountedPrice() {
        DiscountPolicy policy = new DiscountPolicy(4, "Discount policy", EVENT_ID);
        policy.addRule(new PercentageDiscountRule(20.0, "20% off"));
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertEquals(80.0,
                policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 2),
                0.001);
    }

    @Test
    void GivenNoDiscountPolicy_WhenApplyGeneralDiscounts_ThenReturnBasePrice() {
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.empty());

        assertEquals(100.0,
                policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 2),
                0.001);
    }

    // =========================================================================
    // Discount Policy — calculateCouponDiscount
    // =========================================================================

    @Test
    void GivenCouponDiscountAndCorrectCoupon_WhenCalculateCouponDiscount_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(5, "Coupon discount", EVENT_ID);
        policy.addRule(new CouponDiscountRule(20.0, "SAVE20"));
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertEquals(80.0,
                policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, "SAVE20"),
                0.001);
    }

    @Test
    void GivenCouponDiscountAndWrongCoupon_WhenCalculateCouponDiscount_ThenOriginalPriceReturned() {
        DiscountPolicy policy = new DiscountPolicy(5, "Coupon discount", EVENT_ID);
        policy.addRule(new CouponDiscountRule(20.0, "SAVE20"));
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertEquals(100.0,
                policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, "WRONG"),
                0.001);
    }

    @Test
    void GivenEmptyCoupon_WhenCalculateCouponDiscount_ThenRepositoryNotUsedAndOriginalReturned() {
        assertEquals(100.0,
                policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, ""),
                0.001);
        verify(policyRepo, never()).findDiscountPolicyByEventId(any());
    }

    @Test
    void GivenNullCoupon_WhenCalculateCouponDiscount_ThenRepositoryNotUsedAndOriginalReturned() {
        assertEquals(100.0,
                policyService.calculateCouponDiscount(COMPANY_ID, EVENT_ID, 100.0, 2, null),
                0.001);
        verify(policyRepo, never()).findDiscountPolicyByEventId(any());
    }

    // =========================================================================
    // Discount Policy — isConditionalDiscountSatisfied
    // =========================================================================

    @Test
    void GivenConditionalDiscountAndQuantityEnough_WhenCheckConditionalDiscount_ThenReturnTrue() {
        DiscountPolicy policy = new DiscountPolicy(6, "Conditional discount", EVENT_ID);
        policy.addRule(new QuantityConditionalDiscountRule(3, 10.0));
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertTrue(policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 3));
    }

    @Test
    void GivenConditionalDiscountAndQuantityTooLow_WhenCheckConditionalDiscount_ThenReturnFalse() {
        DiscountPolicy policy = new DiscountPolicy(6, "Conditional discount", EVENT_ID);
        policy.addRule(new QuantityConditionalDiscountRule(3, 10.0));
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        assertFalse(policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 2));
    }

    @Test
    void GivenNoDiscountPolicy_WhenCheckConditionalDiscount_ThenReturnFalse() {
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.empty());

        assertFalse(policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 3));
    }

    // =========================================================================
    // Concurrency
    // =========================================================================

    @Test
    @Timeout(5)
    void GivenManyThreads_WhenCheckSelectionPermissionConcurrently_ThenAllResultsConsistent()
            throws Exception {
        SellingPolicy policy = new SellingPolicy(9, "Lottery policy",
                SellingPolicy.SellingType.LOTTERY, EVENT_ID);
        when(policyRepo.findSellingPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                start.await();
                results.add(policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, false));
                done.countDown();
                return null;
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        assertEquals(threads, results.size());
        assertTrue(results.stream().allMatch(r -> !r));
    }

    @Test
    @Timeout(5)
    void GivenManyThreads_WhenApplyDiscountConcurrently_ThenAllResultsConsistent()
            throws Exception {
        DiscountPolicy policy = new DiscountPolicy(10, "Concurrent discount", EVENT_ID);
        policy.addRule(new PercentageDiscountRule(25.0, "25% off"));
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));

        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        ConcurrentLinkedQueue<Double> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                start.await();
                results.add(policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 200.0, 2));
                done.countDown();
                return null;
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        assertEquals(threads, results.size());
        assertTrue(results.stream().allMatch(r -> Math.abs(r - 150.0) < 0.001));
    }
}