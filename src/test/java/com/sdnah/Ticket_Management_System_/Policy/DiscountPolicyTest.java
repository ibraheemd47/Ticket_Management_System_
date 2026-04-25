package com.sdnah.Ticket_Management_System_.Policy;   

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;

import static org.junit.jupiter.api.Assertions.*;


class DiscountPolicyTest {

    private final int TEST_EVENT_ID = 100;

    @Test
    void givenNoDiscountRules_whenCalculateFinalPrice_thenReturnOriginalPrice() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy", TEST_EVENT_ID);
        double result = policy.calculateFinalPrice(100.0, 2, null);
        assertEquals(100.0, result);
    }

    @Test
    void givenNegativeSubtotal_whenCalculateFinalPrice_thenThrowException() {
        DiscountPolicy policy = new DiscountPolicy(1, "discount policy", TEST_EVENT_ID);
        assertThrows(IllegalArgumentException.class,
                () -> policy.calculateFinalPrice(-10.0, 1, null));
    }

    @Test
    void givenConditionalDiscount_whenApply_thenReturnDiscountedPrice() {
        // שימוש במחלקה הפנימית כפי שמוגדרת ב-DiscountPolicy
        DiscountPolicy.ConditionalDiscount discount =
                new DiscountPolicy.ConditionalDiscount(3, 20);

        // המתודה apply מקבלת 3 פרמטרים לפי הקוד שלך
        double result = discount.apply(100.0, 3, null);
        assertEquals(80.0, result, 0.0001);
    }

    @Test
    void givenCouponDiscountAndCorrectCode_whenApply_thenReturnDiscountedPrice() {
        DiscountPolicy.CouponDiscount discount =
                new DiscountPolicy.CouponDiscount("SAVE20", 20.0);

        double result = discount.apply(100.0, 1, "SAVE20");
        assertEquals(80.0, result, 0.0001);
    }
}