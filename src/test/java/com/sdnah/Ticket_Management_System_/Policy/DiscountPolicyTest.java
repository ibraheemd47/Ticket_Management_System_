package com.sdnah.Ticket_Management_System_.Policy;   

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;

import static org.junit.jupiter.api.Assertions.*;

class DiscountPolicyTest {

    @Test
    void givenNoDiscountRules_whenCalculateFinalPrice_thenReturnOriginalPrice() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy");

        double result = policy.calculateFinalPrice(100.0, 2, null);

        assertEquals(100.0, result);
    }

    @Test
    void givenPercentageDiscount_whenCalculateFinalPrice_thenApplyDiscount() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy");

        DiscountPolicy.DiscountRule rule = new DiscountPolicy.PercentageDiscount(10);

        double result = rule.apply(200.0, 1, null);

        assertEquals(180.0, result, 0.0001);
    }

    @Test
    void givenConditionalDiscountAndEnoughQuantity_whenApply_thenApplyDiscount() {
        DiscountPolicy.DiscountRule rule = new DiscountPolicy.ConditionalDiscount(3, 20);

        double result = rule.apply(100.0, 3, null);

        assertEquals(80.0, result, 0.0001);
    }

    @Test
    void givenConditionalDiscountAndNotEnoughQuantity_whenApply_thenReturnOriginalPrice() {
        DiscountPolicy.DiscountRule rule = new DiscountPolicy.ConditionalDiscount(3, 20);

        double result = rule.apply(100.0, 2, null);

        assertEquals(100.0, result, 0.0001);
    }

    @Test
    void givenCouponDiscountWithCorrectCode_whenApply_thenApplyDiscount() {
        DiscountPolicy.DiscountRule rule = new DiscountPolicy.CouponDiscount("SAVE20", 20.0);

        double result = rule.apply(100.0, 1, "SAVE20");

        assertEquals(80.0, result, 0.0001);
    }

    @Test
    void givenCouponDiscountWithWrongCode_whenApply_thenReturnOriginalPrice() {
        DiscountPolicy.DiscountRule rule = new DiscountPolicy.CouponDiscount("SAVE20", 20.0);

        double result = rule.apply(100.0, 1, "WRONG");

        assertEquals(100.0, result, 0.0001);
    }

    @Test
    void givenNegativeSubtotal_whenCalculateFinalPrice_thenThrowException() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy");

        assertThrows(IllegalArgumentException.class,
                () -> policy.calculateFinalPrice(-1.0, 1, null));
    }

    @Test
    void givenPolicyWithoutRules_whenHasDiscounts_thenReturnFalse() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy");

        assertFalse(policy.hasDiscounts());
    }

    @Test
    void givenPolicy_whenIsValidCalled_thenReturnTrueAsImplemented() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy");

        assertTrue(policy.isValid());
    }

    @Test
    void givenPolicy_whenToStringCalled_thenContainCoreFields() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy");

        String result = policy.toString();

        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("discount policy"));
    }
}