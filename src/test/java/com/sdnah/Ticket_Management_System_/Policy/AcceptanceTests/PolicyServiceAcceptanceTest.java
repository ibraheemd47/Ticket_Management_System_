package com.sdnah.Ticket_Management_System_.Policy.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService — Acceptance Tests")
class PolicyServiceAcceptanceTest {

    private static final int EVENT_ID = 100;
    private static final int OTHER_EVENT_ID = 200;

    @Mock
    private IPolicyRepo policyRepo;

    @InjectMocks
    private PolicyService policyService;

    private SellingPolicy regularSellingPolicy;
    private SellingPolicy lotterySellingPolicy;
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;

    @BeforeEach
    void setUp() {
        regularSellingPolicy = new SellingPolicy(
                1,
                "Regular selling policy",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID);

        lotterySellingPolicy = new SellingPolicy(
                2,
                "Lottery selling policy",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID);

        purchasePolicy = new PurchasePolicy(
                3,
                "Default purchase policy",
                EVENT_ID);

        discountPolicy = new DiscountPolicy(
                4,
                "Default discount policy",
                EVENT_ID);
    }

    @Test
    @DisplayName("Given regular selling event and guest, when selecting tickets, then selection is accepted")
    void givenRegularSellingEventAndGuest_WhenSelectingTickets_ThenSelectionIsAccepted() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(regularSellingPolicy));

        // Act
        boolean result = policyService.checkSelectionPermission(EVENT_ID, false);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given lottery selling event and guest, when selecting tickets directly, then selection is rejected")
    void givenLotterySellingEventAndGuest_WhenSelectingTicketsDirectly_ThenSelectionIsRejected() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(lotterySellingPolicy));

        // Act
        boolean result = policyService.checkSelectionPermission(EVENT_ID, false);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given lottery selling event and member, when selecting tickets, then selection is accepted")
    void givenLotterySellingEventAndMember_WhenSelectingTickets_ThenSelectionIsAccepted() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(lotterySellingPolicy));

        // Act
        boolean result = policyService.checkSelectionPermission(EVENT_ID, true);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given no selling policy for event, when selecting tickets, then selection is accepted by default")
    void givenNoSellingPolicyForEvent_WhenSelectingTickets_ThenSelectionIsAcceptedByDefault() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of());

        // Act
        boolean result = policyService.checkSelectionPermission(EVENT_ID, false);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given selling policy belongs to another event, when selecting tickets, then current event uses default behavior")
    void givenSellingPolicyBelongsToAnotherEvent_WhenSelectingTickets_ThenCurrentEventUsesDefaultBehavior() {
        // Arrange
        SellingPolicy otherEventPolicy = new SellingPolicy(
                5,
                "Other event lottery policy",
                SellingPolicy.SellingType.LOTTERY,
                OTHER_EVENT_ID);

        when(policyRepo.findAll()).thenReturn(List.of(otherEventPolicy));

        // Act
        boolean result = policyService.checkSelectionPermission(EVENT_ID, false);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given valid reservation request, when reserving tickets, then reservation is accepted")
    void givenValidReservationRequest_WhenReservingTickets_ThenReservationIsAccepted() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

        // Act
        boolean result = policyService.validateReservationRequest(EVENT_ID, 2, 20);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given zero ticket quantity, when reserving tickets, then reservation is rejected")
    void givenZeroTicketQuantity_WhenReservingTickets_ThenReservationIsRejected() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

        // Act
        boolean result = policyService.validateReservationRequest(EVENT_ID, 0, 20);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given negative ticket quantity, when reserving tickets, then reservation is rejected")
    void givenNegativeTicketQuantity_WhenReservingTickets_ThenReservationIsRejected() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

        // Act
        boolean result = policyService.validateReservationRequest(EVENT_ID, -1, 20);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given no purchase policy for event, when reserving tickets, then reservation is accepted by default")
    void givenNoPurchasePolicyForEvent_WhenReservingTickets_ThenReservationIsAcceptedByDefault() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of());

        // Act
        boolean result = policyService.validateReservationRequest(EVENT_ID, 1, 20);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given valid final purchase conditions, when checkout is requested, then conditions are accepted")
    void givenValidFinalPurchaseConditions_WhenCheckoutRequested_ThenConditionsAreAccepted() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

        // Act
        boolean result = policyService.validateFinalPurchaseConditions(EVENT_ID, 2, 25);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given invalid final purchase quantity, when checkout is requested, then conditions are rejected")
    void givenInvalidFinalPurchaseQuantity_WhenCheckoutRequested_ThenConditionsAreRejected() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(purchasePolicy));

        // Act
        boolean result = policyService.validateFinalPurchaseConditions(EVENT_ID, 0, 25);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given general percentage discount, when checkout applies general discounts, then discounted total is returned")
    void givenGeneralPercentageDiscount_WhenCheckoutAppliesGeneralDiscounts_ThenDiscountedTotalIsReturned() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));
        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

        // Act
        double result = policyService.applyGeneralDiscounts(EVENT_ID, 100.0, 2);

        // Assert
        assertEquals(90.0, result);
    }

    @Test
    @DisplayName("Given no discount policy, when checkout applies general discounts, then original total is returned")
    void givenNoDiscountPolicy_WhenCheckoutAppliesGeneralDiscounts_ThenOriginalTotalIsReturned() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of());

        // Act
        double result = policyService.applyGeneralDiscounts(EVENT_ID, 100.0, 2);

        // Assert
        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Given valid coupon, when checkout applies coupon discount, then coupon discount is applied")
    void givenValidCoupon_WhenCheckoutAppliesCouponDiscount_ThenCouponDiscountIsApplied() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));
        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

        // Act
        double result = policyService.calculateCouponDiscount(EVENT_ID, 100.0, 2, "SAVE20");

        // Assert
        assertEquals(80.0, result);
    }

    @Test
    @DisplayName("Given invalid coupon, when checkout applies coupon discount, then original total is returned")
    void givenInvalidCoupon_WhenCheckoutAppliesCouponDiscount_ThenOriginalTotalIsReturned() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));
        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

        // Act
        double result = policyService.calculateCouponDiscount(EVENT_ID, 100.0, 2, "BAD");

        // Assert
        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Given empty coupon, when checkout applies coupon discount, then original total is returned and repository is not used")
    void givenEmptyCoupon_WhenCheckoutAppliesCouponDiscount_ThenOriginalTotalReturnedAndRepositoryNotUsed() {
        // Act
        double result = policyService.calculateCouponDiscount(EVENT_ID, 100.0, 2, "");

        // Assert
        assertEquals(100.0, result);
        verify(policyRepo, never()).findAll();
    }

    @Test
    @DisplayName("Given null coupon, when checkout applies coupon discount, then original total is returned and repository is not used")
    void givenNullCoupon_WhenCheckoutAppliesCouponDiscount_ThenOriginalTotalReturnedAndRepositoryNotUsed() {
        // Act
        double result = policyService.calculateCouponDiscount(EVENT_ID, 100.0, 2, null);

        // Assert
        assertEquals(100.0, result);
        verify(policyRepo, never()).findAll();
    }

    @Test
    @DisplayName("Given conditional discount and enough quantity, when checking conditional discount, then condition is satisfied")
    void givenConditionalDiscountAndEnoughQuantity_WhenCheckingConditionalDiscount_ThenConditionIsSatisfied() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 25));
        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

        // Act
        boolean result = policyService.isConditionalDiscountSatisfied(EVENT_ID, 3);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given conditional discount and not enough quantity, when checking conditional discount, then condition is not satisfied")
    void givenConditionalDiscountAndNotEnoughQuantity_WhenCheckingConditionalDiscount_ThenConditionIsNotSatisfied() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 25));
        when(policyRepo.findAll()).thenReturn(List.of(discountPolicy));

        // Act
        boolean result = policyService.isConditionalDiscountSatisfied(EVENT_ID, 2);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given no discount policy, when checking conditional discount, then false is returned")
    void givenNoDiscountPolicy_WhenCheckingConditionalDiscount_ThenFalseIsReturned() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of());

        // Act
        boolean result = policyService.isConditionalDiscountSatisfied(EVENT_ID, 3);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given regular event with valid reservation and discount, when completing policy flow, then all checks pass and final price is discounted")
    void givenRegularEventWithValidReservationAndDiscount_WhenCompletingPolicyFlow_ThenAllChecksPassAndFinalPriceIsDiscounted() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        when(policyRepo.findAll()).thenReturn(List.of(
                regularSellingPolicy,
                purchasePolicy,
                discountPolicy));

        // Act
        boolean canSelect = policyService.checkSelectionPermission(EVENT_ID, false);
        boolean canReserve = policyService.validateReservationRequest(EVENT_ID, 2, 21);
        boolean canCheckout = policyService.validateFinalPurchaseConditions(EVENT_ID, 2, 21);
        double finalPrice = policyService.applyGeneralDiscounts(EVENT_ID, 100.0, 2);

        // Assert
        assertTrue(canSelect);
        assertTrue(canReserve);
        assertTrue(canCheckout);
        assertEquals(90.0, finalPrice);
    }

    @Test
    @DisplayName("Given lottery event and guest, when starting policy flow, then selection is rejected")
    void givenLotteryEventAndGuest_WhenStartingPolicyFlow_ThenSelectionIsRejected() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(lotterySellingPolicy));

        // Act
        boolean canSelect = policyService.checkSelectionPermission(EVENT_ID, false);

        // Assert
        assertFalse(canSelect);
    }

    @Test
    @DisplayName("Given regular event but invalid reservation quantity, when completing policy flow, then reservation and checkout are rejected")
    void givenRegularEventButInvalidReservationQuantity_WhenCompletingPolicyFlow_ThenReservationAndCheckoutAreRejected() {
        // Arrange
        when(policyRepo.findAll()).thenReturn(List.of(regularSellingPolicy, purchasePolicy));

        // Act
        boolean canSelect = policyService.checkSelectionPermission(EVENT_ID, false);
        boolean canReserve = policyService.validateReservationRequest(EVENT_ID, 0, 21);
        boolean canCheckout = policyService.validateFinalPurchaseConditions(EVENT_ID, 0, 21);

        // Assert
        assertTrue(canSelect);
        assertFalse(canReserve);
        assertFalse(canCheckout);
    }

    @Test
    @DisplayName("Given change purchase policy request in version 1, when changing policy, then unsupported operation exception is thrown")
    void givenChangePurchasePolicyRequestInVersionOne_WhenChangingPolicy_ThenUnsupportedOperationExceptionIsThrown() {
        // Act
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> policyService.changePurchasePolicy());

        // Assert
        assertEquals("Changing purchase policy is not implemented in Version 1", ex.getMessage());
    }

    @Test
    @DisplayName("Given change discount policy request in version 1, when changing policy, then unsupported operation exception is thrown")
    void givenChangeDiscountPolicyRequestInVersionOne_WhenChangingPolicy_ThenUnsupportedOperationExceptionIsThrown() {
        // Act
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> policyService.changeDiscountPolicy());

        // Assert
        assertEquals("Changing discount policy is not implemented in Version 1", ex.getMessage());
    }
}
