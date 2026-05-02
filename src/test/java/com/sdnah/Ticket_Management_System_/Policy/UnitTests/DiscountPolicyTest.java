package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;

@DisplayName("DiscountPolicy — Domain Unit Tests")
class DiscountPolicyTest {

    private static final int COMPANY_ID = 10;
    private static final int EVENT_ID = 20;

    @Test
    void GivenNoDiscountRules_WhenCalculateFinalPrice_ThenReturnOriginalPrice() {
        DiscountPolicy policy = new DiscountPolicy(1, "No discount", EVENT_ID, COMPANY_ID);

        double result = policy.calculateFinalPrice(100.0, 2, "");

        assertEquals(100.0, result, 0.001);
    }

    @Test
    void GivenPercentageDiscount_WhenCalculateFinalPrice_ThenReturnReducedPrice() {
        DiscountPolicy policy = new DiscountPolicy(1, "Percentage discount", EVENT_ID, COMPANY_ID);
        policy.addDiscountRule(new DiscountPolicy.PercentageDiscount(25));

        double result = policy.calculateFinalPrice(200.0, 2, "");

        assertEquals(150.0, result, 0.001);
    }

    @Test
    void GivenTwoDiscountRules_WhenNotAdditive_ThenBestDiscountChosen() {
        DiscountPolicy policy = new DiscountPolicy(1, "Best discount", EVENT_ID, COMPANY_ID);
        policy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));
        policy.addDiscountRule(new DiscountPolicy.PercentageDiscount(30));

        double result = policy.calculateFinalPrice(100.0, 2, "");

        assertEquals(70.0, result, 0.001);
    }

    @Test
    void GivenConditionalDiscountAndQuantityEnough_WhenCalculateFinalPrice_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(1, "Conditional discount", EVENT_ID, COMPANY_ID);
        policy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 20));

        double result = policy.calculateFinalPrice(100.0, 3, "");

        assertEquals(80.0, result, 0.001);
    }

    @Test
    void GivenConditionalDiscountAndQuantityTooLow_WhenCalculateFinalPrice_ThenOriginalReturned() {
        DiscountPolicy policy = new DiscountPolicy(1, "Conditional discount", EVENT_ID, COMPANY_ID);
        policy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 20));

        double result = policy.calculateFinalPrice(100.0, 2, "");

        assertEquals(100.0, result, 0.001);
    }

    @Test
    void GivenCouponDiscountAndCorrectCoupon_WhenCalculateFinalPrice_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(1, "Coupon discount", EVENT_ID, COMPANY_ID);
        policy.addDiscountRule(new DiscountPolicy.CouponDiscount("CODE", 15));

        double result = policy.calculateFinalPrice(100.0, 1, "CODE");

        assertEquals(85.0, result, 0.001);
    }

    @Test
    void GivenCouponDiscountAndWrongCoupon_WhenCalculateFinalPrice_ThenOriginalReturned() {
        DiscountPolicy policy = new DiscountPolicy(1, "Coupon discount", EVENT_ID, COMPANY_ID);
        policy.addDiscountRule(new DiscountPolicy.CouponDiscount("CODE", 15));

        double result = policy.calculateFinalPrice(100.0, 1, "BAD");

        assertEquals(100.0, result, 0.001);
    }

    @Test
    void GivenCouponDiscountGreaterThanPrice_WhenCalculateFinalPrice_ThenReturnZero() {
        DiscountPolicy policy = new DiscountPolicy(1, "Coupon discount", EVENT_ID, COMPANY_ID);
        policy.addDiscountRule(new DiscountPolicy.CouponDiscount("FREE", 150));

        double result = policy.calculateFinalPrice(100.0, 1, "FREE");

        assertEquals(0.0, result, 0.001);
    }

    @Test
    void GivenNegativeSubtotal_WhenCalculateFinalPrice_ThenThrowIllegalArgumentException() {
        DiscountPolicy policy = new DiscountPolicy(1, "Invalid subtotal", EVENT_ID, COMPANY_ID);

        assertThrows(IllegalArgumentException.class, () ->
                policy.calculateFinalPrice(-1.0, 1, "")
        );
    }

    @Test
    void GivenNullDiscountRule_WhenAddDiscountRule_ThenRuleIgnored() {
        DiscountPolicy policy = new DiscountPolicy(1, "Null rule", EVENT_ID, COMPANY_ID);

        policy.addDiscountRule(null);

        assertFalse(policy.hasDiscounts());
    }

    @Test
    void GivenValidDiscountRule_WhenAddDiscountRule_ThenHasDiscountsTrue() {
        DiscountPolicy policy = new DiscountPolicy(1, "Has discount", EVENT_ID, COMPANY_ID);

        policy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        assertTrue(policy.hasDiscounts());
    }

    @Test
    void GivenConditionalDiscount_WhenQuantityEnough_ThenConditionalSatisfied() {
        DiscountPolicy policy = new DiscountPolicy(1, "Conditional", EVENT_ID, COMPANY_ID);
        policy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(2, 10));

        assertTrue(policy.isAnyConditionalDiscountSatisfied(2));
    }

    @Test
    void GivenOnlyPercentageDiscount_WhenCheckConditionalSatisfied_ThenReturnFalse() {
        DiscountPolicy policy = new DiscountPolicy(1, "Percentage", EVENT_ID, COMPANY_ID);
        policy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        assertFalse(policy.isAnyConditionalDiscountSatisfied(5));
    }
}