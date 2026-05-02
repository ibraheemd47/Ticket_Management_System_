package com.sdnah.Ticket_Management_System_.Policy.AcceptanceTests;


import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PolicyService — Acceptance Tests")
@ExtendWith(MockitoExtension.class)
class PolicyServiceAcceptanceTest {

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
    // Acceptance tests access the system only through Application Layer.
    // =========================================================================

    @Test
    @DisplayName("GivenRegularSellingPolicy_WhenGuestSelectsTickets_ThenSelectionApproved")
    void GivenRegularSellingPolicy_WhenGuestSelectsTickets_ThenSelectionApproved() {
        SellingPolicy sellingPolicy = new SellingPolicy(
                1,
                "Regular selling policy",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(sellingPolicy));

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
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(sellingPolicy));

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
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(sellingPolicy));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, EVENT_ID, true);

        assertTrue(result);
    }

    // =========================================================================
    // UC II.2.4 — Reserve Tickets in Active Order
    // =========================================================================

    @Test
    @DisplayName("GivenValidPurchasePolicy_WhenGuestReservesTickets_ThenReservationApproved")
    void GivenValidPurchasePolicy_WhenGuestReservesTickets_ThenReservationApproved() {
        PurchasePolicy purchasePolicy = new PurchasePolicy(
                4,
                "Default purchase policy",
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 2, 18);

        assertTrue(result);
    }

    @Test
    @DisplayName("GivenPurchasePolicy_WhenGuestReservesZeroTickets_ThenReservationRejected")
    void GivenPurchasePolicy_WhenGuestReservesZeroTickets_ThenReservationRejected() {
        PurchasePolicy purchasePolicy = new PurchasePolicy(
                5,
                "Default purchase policy",
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 0, 18);

        assertFalse(result);
    }

    @Test
    @DisplayName("GivenNoPurchasePolicy_WhenGuestReservesTickets_ThenDefaultReservationApproved")
    void GivenNoPurchasePolicy_WhenGuestReservesTickets_ThenDefaultReservationApproved() {
        when(policyRepo.findAll()).thenReturn(List.of());

        boolean result = policyService.validateReservationRequest(COMPANY_ID, EVENT_ID, 2, 18);

        assertTrue(result);
    }

    // =========================================================================
    // UC II.2.8 — Checkout Active Order
    // =========================================================================

    @Test
    @DisplayName("GivenValidPurchasePolicy_WhenCheckoutActiveOrder_ThenFinalPurchaseApproved")
    void GivenValidPurchasePolicy_WhenCheckoutActiveOrder_ThenFinalPurchaseApproved() {
        PurchasePolicy purchasePolicy = new PurchasePolicy(
                6,
                "Checkout purchase policy",
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

        boolean result = policyService.validateFinalPurchaseConditions(COMPANY_ID, EVENT_ID, 2, 18);

        assertTrue(result);
    }

    @Test
    @DisplayName("GivenPurchasePolicy_WhenCheckoutWithZeroTickets_ThenFinalPurchaseRejected")
    void GivenPurchasePolicy_WhenCheckoutWithZeroTickets_ThenFinalPurchaseRejected() {
        PurchasePolicy purchasePolicy = new PurchasePolicy(
                7,
                "Checkout purchase policy",
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

        boolean result = policyService.validateFinalPurchaseConditions(COMPANY_ID, EVENT_ID, 0, 18);

        assertFalse(result);
    }

    @Test
    @DisplayName("GivenPercentageDiscountPolicy_WhenCheckoutActiveOrder_ThenFinalPriceReduced")
    void GivenPercentageDiscountPolicy_WhenCheckoutActiveOrder_ThenFinalPriceReduced() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                8,
                "General discount policy",
                EVENT_ID,
                COMPANY_ID
        );
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(20));

        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

        double result = policyService.applyGeneralDiscounts(COMPANY_ID, EVENT_ID, 100.0, 2);

        assertEquals(80.0, result, 0.001);
    }

    @Test
    @DisplayName("GivenCouponDiscountPolicy_WhenCheckoutWithValidCoupon_ThenCouponDiscountApplied")
    void GivenCouponDiscountPolicy_WhenCheckoutWithValidCoupon_ThenCouponDiscountApplied() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                9,
                "Coupon discount policy",
                EVENT_ID,
                COMPANY_ID
        );
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));

        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

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
                EVENT_ID,
                COMPANY_ID
        );
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));

        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

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
                EVENT_ID,
                COMPANY_ID
        );
        discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 10));

        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

        boolean result = policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 3);

        assertTrue(result);
    }

    @Test
    @DisplayName("GivenConditionalDiscountPolicy_WhenQuantityDoesNotMeetCondition_ThenConditionNotSatisfied")
    void GivenConditionalDiscountPolicy_WhenQuantityDoesNotMeetCondition_ThenConditionNotSatisfied() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                12,
                "Conditional discount policy",
                EVENT_ID,
                COMPANY_ID
        );
        discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 10));

        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

        boolean result = policyService.isConditionalDiscountSatisfied(COMPANY_ID, EVENT_ID, 2);

        assertFalse(result);
    }

    // =========================================================================
    // Company default policy fallback
    // Requires eventId constructor parameter to be Integer, not int.
    // =========================================================================

    @Test
    @DisplayName("GivenEventPolicyAndCompanyPolicy_WhenApplyingDiscount_ThenEventPolicyHasPriority")
    void GivenEventPolicyAndCompanyPolicy_WhenApplyingDiscount_ThenEventPolicyHasPriority() {
        DiscountPolicy companyPolicy = new DiscountPolicy(
                13,
                "Company default discount",
                null,
                COMPANY_ID
        );
        companyPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        DiscountPolicy eventPolicy = new DiscountPolicy(
                14,
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
    @DisplayName("GivenOnlyCompanyPolicy_WhenApplyingDiscount_ThenCompanyPolicyUsed")
    void GivenOnlyCompanyPolicy_WhenApplyingDiscount_ThenCompanyPolicyUsed() {
        DiscountPolicy companyPolicy = new DiscountPolicy(
                15,
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
    // UC II.4.3 — Change Purchase and Discount Policies
    // Not implemented in Version 1.
    // =========================================================================

    @Test
    @DisplayName("GivenVersionOne_WhenChangePurchasePolicyRequested_ThenUnsupportedOperationThrown")
    void GivenVersionOne_WhenChangePurchasePolicyRequested_ThenUnsupportedOperationThrown() {
        assertThrows(UnsupportedOperationException.class, () -> policyService.changePurchasePolicy());
    }

    @Test
    @DisplayName("GivenVersionOne_WhenChangeDiscountPolicyRequested_ThenUnsupportedOperationThrown")
    void GivenVersionOne_WhenChangeDiscountPolicyRequested_ThenUnsupportedOperationThrown() {
        assertThrows(UnsupportedOperationException.class, () -> policyService.changeDiscountPolicy());
    }

    // =========================================================================
    // Concurrency Acceptance Test
    // ExecutorService + CountDownLatch, as required.
    // =========================================================================

    @Test
    @Timeout(5)
    @DisplayName("GivenManyConcurrentUsers_WhenCheckingLotterySelection_ThenAllGuestRequestsRejectedConsistently")
    void GivenManyConcurrentUsers_WhenCheckingLotterySelection_ThenAllGuestRequestsRejectedConsistently() throws Exception {
        SellingPolicy sellingPolicy = new SellingPolicy(
                16,
                "Lottery selling policy",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID,
                COMPANY_ID
        );

        when(policyRepo.findAll()).thenReturn(List.of(sellingPolicy));

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

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// import java.util.List;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;

// @ExtendWith(MockitoExtension.class)
// @DisplayName("PolicyService — Acceptance Tests")
// class PolicyServiceAcceptanceTest {

//     private static final int EVENT_ID = 100;
//     private static final int OTHER_EVENT_ID = 200;

//     @Mock
//     private IPolicyRepo policyRepo;

//     @InjectMocks
//     private PolicyService policyService;

//     private SellingPolicy regularSellingPolicy;
//     private SellingPolicy lotterySellingPolicy;
//     private PurchasePolicy purchasePolicy;
//     private DiscountPolicy discountPolicy;

//     @BeforeEach
//     void setUp() {
//         regularSellingPolicy = new SellingPolicy(
//                 1,
//                 "Regular selling policy",
//                 SellingPolicy.SellingType.REGULAR,
//                 EVENT_ID);

//         lotterySellingPolicy = new SellingPolicy(
//                 2,
//                 "Lottery selling policy",
//                 SellingPolicy.SellingType.LOTTERY,
//                 EVENT_ID);

//         purchasePolicy = new PurchasePolicy(
//                 3,
//                 "Default purchase policy",
//                 EVENT_ID);

//         discountPolicy = new DiscountPolicy(
//                 4,
//                 "Default discount policy",
//                 EVENT_ID);
//     }

//     @Test
//     @DisplayName("Given regular selling event and guest, when selecting tickets, then selection is accepted")
//     void givenRegularSellingEventAndGuest_WhenSelectingTickets_ThenSelectionIsAccepted() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(regularSellingPolicy));

//         // Act
//         boolean result = policyService.checkSelectionPermission(EVENT_ID, false);

//         // Assert
//         assertTrue(result);
//     }

//     @Test
//     @DisplayName("Given lottery selling event and guest, when selecting tickets directly, then selection is rejected")
//     void givenLotterySellingEventAndGuest_WhenSelectingTicketsDirectly_ThenSelectionIsRejected() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(lotterySellingPolicy));

//         // Act
//         boolean result = policyService.checkSelectionPermission(EVENT_ID, false);

//         // Assert
//         assertFalse(result);
//     }

//     @Test
//     @DisplayName("Given lottery selling event and member, when selecting tickets, then selection is accepted")
//     void givenLotterySellingEventAndMember_WhenSelectingTickets_ThenSelectionIsAccepted() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(lotterySellingPolicy));

//         // Act
//         boolean result = policyService.checkSelectionPermission(EVENT_ID, true);

//         // Assert
//         assertTrue(result);
//     }

//     @Test
//     @DisplayName("Given no selling policy for event, when selecting tickets, then selection is accepted by default")
//     void givenNoSellingPolicyForEvent_WhenSelectingTickets_ThenSelectionIsAcceptedByDefault() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of());

//         // Act
//         boolean result = policyService.checkSelectionPermission(EVENT_ID, false);

//         // Assert
//         assertTrue(result);
//     }

//     @Test
//     @DisplayName("Given selling policy belongs to another event, when selecting tickets, then current event uses default behavior")
//     void givenSellingPolicyBelongsToAnotherEvent_WhenSelectingTickets_ThenCurrentEventUsesDefaultBehavior() {
//         // Arrange
//         SellingPolicy otherEventPolicy = new SellingPolicy(
//                 5,
//                 "Other event lottery policy",
//                 SellingPolicy.SellingType.LOTTERY,
//                 OTHER_EVENT_ID);

//         when(policyRepo.findAll()).thenReturn(List.of(otherEventPolicy));

//         // Act
//         boolean result = policyService.checkSelectionPermission(EVENT_ID, false);

//         // Assert
//         assertTrue(result);
//     }

//     @Test
//     @DisplayName("Given valid reservation request, when reserving tickets, then reservation is accepted")
//     void givenValidReservationRequest_WhenReservingTickets_ThenReservationIsAccepted() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

//         // Act
//         boolean result = policyService.validateReservationRequest(EVENT_ID, 2, 20);

//         // Assert
//         assertTrue(result);
//     }

//     @Test
//     @DisplayName("Given zero ticket quantity, when reserving tickets, then reservation is rejected")
//     void givenZeroTicketQuantity_WhenReservingTickets_ThenReservationIsRejected() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

//         // Act
//         boolean result = policyService.validateReservationRequest(EVENT_ID, 0, 20);

//         // Assert
//         assertFalse(result);
//     }

//     @Test
//     @DisplayName("Given negative ticket quantity, when reserving tickets, then reservation is rejected")
//     void givenNegativeTicketQuantity_WhenReservingTickets_ThenReservationIsRejected() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

//         // Act
//         boolean result = policyService.validateReservationRequest(EVENT_ID, -1, 20);

//         // Assert
//         assertFalse(result);
//     }

//     @Test
//     @DisplayName("Given no purchase policy for event, when reserving tickets, then reservation is accepted by default")
//     void givenNoPurchasePolicyForEvent_WhenReservingTickets_ThenReservationIsAcceptedByDefault() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of());

//         // Act
//         boolean result = policyService.validateReservationRequest(EVENT_ID, 1, 20);

//         // Assert
//         assertTrue(result);
//     }

//     @Test
//     @DisplayName("Given valid final purchase conditions, when checkout is requested, then conditions are accepted")
//     void givenValidFinalPurchaseConditions_WhenCheckoutRequested_ThenConditionsAreAccepted() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

//         // Act
//         boolean result = policyService.validateFinalPurchaseConditions(EVENT_ID, 2, 25);

//         // Assert
//         assertTrue(result);
//     }

//     @Test
//     @DisplayName("Given invalid final purchase quantity, when checkout is requested, then conditions are rejected")
//     void givenInvalidFinalPurchaseQuantity_WhenCheckoutRequested_ThenConditionsAreRejected() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

//         // Act
//         boolean result = policyService.validateFinalPurchaseConditions(EVENT_ID, 0, 25);

//         // Assert
//         assertFalse(result);
//     }

//     @Test
//     @DisplayName("Given general percentage discount, when checkout applies general discounts, then discounted total is returned")
//     void givenGeneralPercentageDiscount_WhenCheckoutAppliesGeneralDiscounts_ThenDiscountedTotalIsReturned() {
//         // Arrange
//         discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));
//         when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

//         // Act
//         double result = policyService.applyGeneralDiscounts(EVENT_ID, 100.0, 2);

//         // Assert
//         assertEquals(90.0, result);
//     }

//     @Test
//     @DisplayName("Given no discount policy, when checkout applies general discounts, then original total is returned")
//     void givenNoDiscountPolicy_WhenCheckoutAppliesGeneralDiscounts_ThenOriginalTotalIsReturned() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of());

//         // Act
//         double result = policyService.applyGeneralDiscounts(EVENT_ID, 100.0, 2);

//         // Assert
//         assertEquals(100.0, result);
//     }

//     @Test
//     @DisplayName("Given valid coupon, when checkout applies coupon discount, then coupon discount is applied")
//     void givenValidCoupon_WhenCheckoutAppliesCouponDiscount_ThenCouponDiscountIsApplied() {
//         // Arrange
//         discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));
//         when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

//         // Act
//         double result = policyService.calculateCouponDiscount(EVENT_ID, 100.0, 2, "SAVE20");

//         // Assert
//         assertEquals(80.0, result);
//     }

//     @Test
//     @DisplayName("Given invalid coupon, when checkout applies coupon discount, then original total is returned")
//     void givenInvalidCoupon_WhenCheckoutAppliesCouponDiscount_ThenOriginalTotalIsReturned() {
//         // Arrange
//         discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));
//         when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

//         // Act
//         double result = policyService.calculateCouponDiscount(EVENT_ID, 100.0, 2, "BAD");

//         // Assert
//         assertEquals(100.0, result);
//     }

//     @Test
//     @DisplayName("Given empty coupon, when checkout applies coupon discount, then original total is returned and repository is not used")
//     void givenEmptyCoupon_WhenCheckoutAppliesCouponDiscount_ThenOriginalTotalReturnedAndRepositoryNotUsed() {
//         // Act
//         double result = policyService.calculateCouponDiscount(EVENT_ID, 100.0, 2, "");

//         // Assert
//         assertEquals(100.0, result);
//         verify(policyRepo, never()).findAll();
//     }

//     @Test
//     @DisplayName("Given null coupon, when checkout applies coupon discount, then original total is returned and repository is not used")
//     void givenNullCoupon_WhenCheckoutAppliesCouponDiscount_ThenOriginalTotalReturnedAndRepositoryNotUsed() {
//         // Act
//         double result = policyService.calculateCouponDiscount(EVENT_ID, 100.0, 2, null);

//         // Assert
//         assertEquals(100.0, result);
//         verify(policyRepo, never()).findAll();
//     }

//     @Test
//     @DisplayName("Given conditional discount and enough quantity, when checking conditional discount, then condition is satisfied")
//     void givenConditionalDiscountAndEnoughQuantity_WhenCheckingConditionalDiscount_ThenConditionIsSatisfied() {
//         // Arrange
//         discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 25));
//         when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

//         // Act
//         boolean result = policyService.isConditionalDiscountSatisfied(EVENT_ID, 3);

//         // Assert
//         assertTrue(result);
//     }

//     @Test
//     @DisplayName("Given conditional discount and not enough quantity, when checking conditional discount, then condition is not satisfied")
//     void givenConditionalDiscountAndNotEnoughQuantity_WhenCheckingConditionalDiscount_ThenConditionIsNotSatisfied() {
//         // Arrange
//         discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 25));
//         when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

//         // Act
//         boolean result = policyService.isConditionalDiscountSatisfied(EVENT_ID, 2);

//         // Assert
//         assertFalse(result);
//     }

//     @Test
//     @DisplayName("Given no discount policy, when checking conditional discount, then false is returned")
//     void givenNoDiscountPolicy_WhenCheckingConditionalDiscount_ThenFalseIsReturned() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of());

//         // Act
//         boolean result = policyService.isConditionalDiscountSatisfied(EVENT_ID, 3);

//         // Assert
//         assertFalse(result);
//     }

//     @Test
//     @DisplayName("Given regular event with valid reservation and discount, when completing policy flow, then all checks pass and final price is discounted")
//     void givenRegularEventWithValidReservationAndDiscount_WhenCompletingPolicyFlow_ThenAllChecksPassAndFinalPriceIsDiscounted() {
//         // Arrange
//         discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

//         when(policyRepo.findAll()).thenReturn(List.of(
//                 regularSellingPolicy,
//                 purchasePolicy,
//                 discountPolicy));

//         // Act
//         boolean canSelect = policyService.checkSelectionPermission(EVENT_ID, false);
//         boolean canReserve = policyService.validateReservationRequest(EVENT_ID, 2, 21);
//         boolean canCheckout = policyService.validateFinalPurchaseConditions(EVENT_ID, 2, 21);
//         double finalPrice = policyService.applyGeneralDiscounts(EVENT_ID, 100.0, 2);

//         // Assert
//         assertTrue(canSelect);
//         assertTrue(canReserve);
//         assertTrue(canCheckout);
//         assertEquals(90.0, finalPrice);
//     }

//     @Test
//     @DisplayName("Given lottery event and guest, when starting policy flow, then selection is rejected")
//     void givenLotteryEventAndGuest_WhenStartingPolicyFlow_ThenSelectionIsRejected() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(lotterySellingPolicy));

//         // Act
//         boolean canSelect = policyService.checkSelectionPermission(EVENT_ID, false);

//         // Assert
//         assertFalse(canSelect);
//     }

//     @Test
//     @DisplayName("Given regular event but invalid reservation quantity, when completing policy flow, then reservation and checkout are rejected")
//     void givenRegularEventButInvalidReservationQuantity_WhenCompletingPolicyFlow_ThenReservationAndCheckoutAreRejected() {
//         // Arrange
//         when(policyRepo.findAll()).thenReturn(List.of(regularSellingPolicy, purchasePolicy));

//         // Act
//         boolean canSelect = policyService.checkSelectionPermission(EVENT_ID, false);
//         boolean canReserve = policyService.validateReservationRequest(EVENT_ID, 0, 21);
//         boolean canCheckout = policyService.validateFinalPurchaseConditions(EVENT_ID, 0, 21);

//         // Assert
//         assertTrue(canSelect);
//         assertFalse(canReserve);
//         assertFalse(canCheckout);
//     }

//     @Test
//     @DisplayName("Given change purchase policy request in version 1, when changing policy, then unsupported operation exception is thrown")
//     void givenChangePurchasePolicyRequestInVersionOne_WhenChangingPolicy_ThenUnsupportedOperationExceptionIsThrown() {
//         // Act
//         UnsupportedOperationException ex = assertThrows(
//                 UnsupportedOperationException.class,
//                 () -> policyService.changePurchasePolicy());

//         // Assert
//         assertEquals("Changing purchase policy is not implemented in Version 1", ex.getMessage());
//     }

//     @Test
//     @DisplayName("Given change discount policy request in version 1, when changing policy, then unsupported operation exception is thrown")
//     void givenChangeDiscountPolicyRequestInVersionOne_WhenChangingPolicy_ThenUnsupportedOperationExceptionIsThrown() {
//         // Act
//         UnsupportedOperationException ex = assertThrows(
//                 UnsupportedOperationException.class,
//                 () -> policyService.changeDiscountPolicy());

//         // Assert
//         assertEquals("Changing discount policy is not implemented in Version 1", ex.getMessage());
//     }
// }
