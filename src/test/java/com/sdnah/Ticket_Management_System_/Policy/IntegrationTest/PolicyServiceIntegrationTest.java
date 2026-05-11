package com.sdnah.Ticket_Management_System_.Policy.IntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

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

    @Test
    @DisplayName("Given regular selling policy, when guest selects tickets, then selection is approved")
    void givenRegularSellingPolicy_WhenGuestSelectsTickets_ThenSelectionIsApproved() {
        policyRepository.saveAndFlush(new SellingPolicy(
                1,
                "Regular policy",
                SellingPolicy.SellingType.REGULAR,
                eventId));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, eventId, false);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given lottery selling policy, when guest selects tickets, then selection is rejected")
    void givenLotterySellingPolicy_WhenGuestSelectsTickets_ThenSelectionIsRejected() {
        policyRepository.saveAndFlush(new SellingPolicy(
                2,
                "Lottery policy",
                SellingPolicy.SellingType.LOTTERY,
                eventId));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, eventId, false);

        assertFalse(result);
    }

    @Test
    @DisplayName("Given lottery selling policy, when member selects tickets, then selection is approved")
    void givenLotterySellingPolicy_WhenMemberSelectsTickets_ThenSelectionIsApproved() {
        policyRepository.saveAndFlush(new SellingPolicy(
                3,
                "Lottery policy",
                SellingPolicy.SellingType.LOTTERY,
                eventId));

        boolean result = policyService.checkSelectionPermission(COMPANY_ID, eventId, true);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given no selling policy, when checking selection, then default approves selection")
    void givenNoSellingPolicy_WhenCheckingSelection_ThenDefaultApprovesSelection() {
        boolean result = policyService.checkSelectionPermission(COMPANY_ID, eventId, false);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given purchase policy, when validating valid reservation, then approved")
    void givenPurchasePolicy_WhenValidatingValidReservation_ThenApproved() {
        policyRepository.saveAndFlush(new PurchasePolicy(
                4,
                "Standard purchase",
                eventId));

        boolean result = policyService.validateReservationRequest(COMPANY_ID, eventId, 2, 25);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given purchase policy with max tickets, when quantity exceeds max, then rejected")
    void givenPurchasePolicyWithMaxTickets_WhenQuantityExceedsMax_ThenRejected() {
        PurchasePolicy policy = new PurchasePolicy(
                5,
                "Max 2 tickets",
                eventId);

        policy.setMaxTickets(2);

        policyRepository.saveAndFlush(policy);

        boolean result = policyService.validateReservationRequest(COMPANY_ID, eventId, 3, 25);

        assertFalse(result);
    }

    @Test
    @DisplayName("Given no purchase policy, when validating reservation, then default approves reservation")
    void givenNoPurchasePolicy_WhenValidatingReservation_ThenDefaultApprovesReservation() {
        boolean result = policyService.validateReservationRequest(COMPANY_ID, eventId, 2, 25);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given no discount policy, when applying general discounts, then base price is unchanged")
    void givenNoDiscountPolicy_WhenApplyingGeneralDiscounts_ThenBasePriceIsUnchanged() {
        double result = policyService.applyGeneralDiscounts(COMPANY_ID, eventId, 100.0, 1);

        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Given discount policy with percentage rule, when applying general discounts, then price is reduced")
    void givenDiscountPolicyWithPercentageRule_WhenApplyingGeneralDiscounts_ThenPriceIsReduced() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                6,
                "10 percent off",
                eventId);

        discountPolicy.addDiscount(new DiscountPolicy.PercentageDiscount(10));

        policyRepository.saveAndFlush(discountPolicy);

        double result = policyService.applyGeneralDiscounts(COMPANY_ID, eventId, 100.0, 1);

        assertEquals(90.0, result);
    }

    @Test
    @DisplayName("Given coupon discount, when applying matching coupon, then discount is applied")
    void givenCouponDiscount_WhenApplyingMatchingCoupon_ThenDiscountIsApplied() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                7,
                "Coupon SAVE10",
                eventId);

        discountPolicy.addDiscount(new DiscountPolicy.CouponDiscount("SAVE10", 10.0));

        policyRepository.saveAndFlush(discountPolicy);

        double result = policyService.calculateCouponDiscount(
                COMPANY_ID,
                eventId,
                100.0,
                1,
                "SAVE10");

        assertEquals(90.0, result);
    }

    @Test
    @DisplayName("Given blank coupon, when calculating coupon discount, then price is unchanged")
    void givenBlankCoupon_WhenCalculatingCouponDiscount_ThenPriceIsUnchanged() {
        double result = policyService.calculateCouponDiscount(
                COMPANY_ID,
                eventId,
                100.0,
                1,
                "   ");

        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Given conditional discount with quantity threshold met, when checking, then satisfied")
    void givenConditionalDiscountWithQuantityThresholdMet_WhenChecking_ThenSatisfied() {
        DiscountPolicy discountPolicy = new DiscountPolicy(
                8,
                "Buy 3 get discount",
                eventId);

        discountPolicy.addDiscount(new DiscountPolicy.ConditionalDiscount(3, 20.0));

        policyRepository.saveAndFlush(discountPolicy);

        assertTrue(policyService.isConditionalDiscountSatisfied(COMPANY_ID, eventId, 3));
        assertFalse(policyService.isConditionalDiscountSatisfied(COMPANY_ID, eventId, 2));
    }

    @Test
    @DisplayName("Given no discount policy, when checking conditional discount, then false is returned")
    void givenNoDiscountPolicy_WhenCheckingConditionalDiscount_ThenFalseIsReturned() {
        boolean result = policyService.isConditionalDiscountSatisfied(COMPANY_ID, eventId, 3);

        assertFalse(result);
    }

    @Test
    @DisplayName("Given final purchase conditions with purchase policy, when valid, then approved")
    void givenFinalPurchaseConditionsWithPurchasePolicy_WhenValid_ThenApproved() {
        policyRepository.saveAndFlush(new PurchasePolicy(
                9,
                "Final purchase policy",
                eventId));

        boolean result = policyService.validateFinalPurchaseConditions(COMPANY_ID, eventId, 2);

        assertTrue(result);
    }

    @Test
    @DisplayName("Given changePurchasePolicy called, when invoked, then UnsupportedOperationException is thrown")
    void givenChangePurchasePolicyCalled_WhenInvoked_ThenUnsupportedOperationExceptionIsThrown() {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> policyService.changePurchasePolicy());

        assertEquals("Not implemented", ex.getMessage());
    }

    @Test
    @DisplayName("Given changeDiscountPolicy called, when invoked, then UnsupportedOperationException is thrown")
    void givenChangeDiscountPolicyCalled_WhenInvoked_ThenUnsupportedOperationExceptionIsThrown() {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> policyService.changeDiscountPolicy());

        assertEquals("Not implemented", ex.getMessage());
    }
}