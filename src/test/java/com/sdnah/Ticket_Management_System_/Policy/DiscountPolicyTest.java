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
    void givenNegativeSubtotal_whenCalculateFinalPrice_thenThrowException() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy");

        assertThrows(IllegalArgumentException.class,
                () -> policy.calculateFinalPrice(-10.0, 1, null));
    }

    @Test
    void givenPolicyWithoutRules_whenHasDiscounts_thenReturnFalse() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy");

        assertFalse(policy.hasDiscounts());
    }

    @Test
    void givenDiscountPolicy_whenIsValidCalled_thenReturnTrueAsImplemented() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy");

        assertTrue(policy.isValid());
    }

    @Test
    void givenPolicy_whenToStringCalled_thenContainIdAndDescription() {
        DiscountPolicy policy = new DiscountPolicy(5, "summer sale");

        String result = policy.toString();

        assertTrue(result.contains("id=5"));
        assertTrue(result.contains("summer sale"));
    }

    @Test
    void givenPercentageDiscount_whenApply_thenReturnDiscountedPrice() {
        DiscountPolicy.PercentageDiscount discount =
                new DiscountPolicy.PercentageDiscount(10);

        double result = discount.apply(200.0, 1, null);

        assertEquals(180.0, result, 0.0001);
    }

    @Test
    void givenPercentageDiscountZero_whenApply_thenReturnSamePrice() {
        DiscountPolicy.PercentageDiscount discount =
                new DiscountPolicy.PercentageDiscount(0);

        double result = discount.apply(200.0, 1, null);

        assertEquals(200.0, result, 0.0001);
    }

    @Test
    void givenConditionalDiscountAndEnoughQuantity_whenApply_thenReturnDiscountedPrice() {
        DiscountPolicy.ConditionalDiscount discount =
                new DiscountPolicy.ConditionalDiscount(3, 20);

        double result = discount.apply(100.0, 3, null);

        assertEquals(80.0, result, 0.0001);
    }

    @Test
    void givenConditionalDiscountAndInsufficientQuantity_whenApply_thenReturnOriginalPrice() {
        DiscountPolicy.ConditionalDiscount discount =
                new DiscountPolicy.ConditionalDiscount(3, 20);

        double result = discount.apply(100.0, 2, null);

        assertEquals(100.0, result, 0.0001);
    }

    @Test
    void givenCouponDiscountAndCorrectCode_whenApply_thenReturnDiscountedPrice() {
        DiscountPolicy.CouponDiscount discount =
                new DiscountPolicy.CouponDiscount("SAVE20", 20.0);

        double result = discount.apply(100.0, 1, "SAVE20");

        assertEquals(80.0, result, 0.0001);
    }

    @Test
    void givenCouponDiscountAndWrongCode_whenApply_thenReturnOriginalPrice() {
        DiscountPolicy.CouponDiscount discount =
                new DiscountPolicy.CouponDiscount("SAVE20", 20.0);

        double result = discount.apply(100.0, 1, "WRONG");

        assertEquals(100.0, result, 0.0001);
    }

    @Test
    void givenCouponDiscountAndMatchingCodeWithLargeAmount_whenApply_thenNotDropBelowZero() {
        DiscountPolicy.CouponDiscount discount =
                new DiscountPolicy.CouponDiscount("SAVE200", 200.0);

        double result = discount.apply(100.0, 1, "SAVE200");

        assertEquals(0.0, result, 0.0001);
    }
}