package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DiscountPolicy — Domain Unit Tests")
class DiscountPolicyTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    private DiscountContext ctx(int qty) {
        return new DiscountContext(qty, LocalDateTime.now());
    }

   

    // =========================================================================
    // calculateFinalPrice
    // =========================================================================

    @Test
    void GivenNoDiscountRules_WhenCalculateFinalPrice_ThenReturnOriginalPrice() {
        DiscountPolicy policy = new DiscountPolicy(1, "No discount", EVENT_ID, COMPANY_ID);

        assertEquals(100.0, policy.calculateFinalPrice(100.0, 2, ""), 0.001);
    }

    @Test
    void GivenPercentageDiscount_WhenCalculateFinalPrice_ThenReturnReducedPrice() {
        DiscountPolicy policy = new DiscountPolicy(1, "Percentage discount", EVENT_ID, COMPANY_ID);
        policy.addRule(new PercentageDiscountRule(25.0, "25% off"));

        assertEquals(150.0, policy.calculateFinalPrice(200.0, 2, ""), 0.001);
    }

    @Test
    void GivenTwoDiscountRules_WhenNotAdditive_ThenBestDiscountChosen() {
        DiscountPolicy policy = new DiscountPolicy(1, "Best discount", EVENT_ID, COMPANY_ID);
        policy.addRule(new PercentageDiscountRule(10.0, "10% off"));
        policy.addRule(new PercentageDiscountRule(30.0, "30% off"));

        // MAX: best of 10% and 30% → 30% → 100 * 0.7 = 70
        assertEquals(70.0, policy.calculateFinalPrice(100.0, 2, ""), 0.001);
    }

    @Test
    void GivenTwoDiscountRules_WhenAdditive_ThenApplyCumulative() {
        DiscountPolicy policy = new DiscountPolicy(1, "Additive discount", EVENT_ID, COMPANY_ID);
        policy.setRules(
                java.util.List.of(
                        new PercentageDiscountRule(10.0, "10% off"),
                        new PercentageDiscountRule(20.0, "20% off")),
                true);

        // SUM: 10 + 20 = 30% → 100 * 0.7 = 70
        assertEquals(70.0, policy.calculateFinalPrice(100.0, 2, ""), 0.001);
    }

    @Test
    void GivenConditionalDiscountAndQuantityEnough_WhenCalculateFinalPrice_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(1, "Conditional discount", EVENT_ID, COMPANY_ID);
        policy.addRule(new QuantityConditionalDiscountRule(3, 20.0));

        assertEquals(80.0, policy.calculateFinalPrice(100.0, 3, ""), 0.001);
    }

    @Test
    void GivenConditionalDiscountAndQuantityTooLow_WhenCalculateFinalPrice_ThenOriginalReturned() {
        DiscountPolicy policy = new DiscountPolicy(1, "Conditional discount", EVENT_ID, COMPANY_ID);
        policy.addRule(new QuantityConditionalDiscountRule(3, 20.0));

        assertEquals(100.0, policy.calculateFinalPrice(100.0, 2, ""), 0.001);
    }

    @Test
    void GivenCouponDiscountAndCorrectCoupon_WhenCalculateFinalPrice_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(1, "Coupon discount", EVENT_ID, COMPANY_ID);
        policy.addRule(new CouponDiscountRule(15.0, "CODE"));

        assertEquals(85.0, policy.calculateFinalPrice(100.0, 1, "CODE"), 0.001);
    }

    @Test
    void GivenCouponDiscountAndWrongCoupon_WhenCalculateFinalPrice_ThenOriginalReturned() {
        DiscountPolicy policy = new DiscountPolicy(1, "Coupon discount", EVENT_ID, COMPANY_ID);
        policy.addRule(new CouponDiscountRule(15.0, "CODE"));

        assertEquals(100.0, policy.calculateFinalPrice(100.0, 1, "BAD"), 0.001);
    }

    @Test
    void GivenCouponDiscountGreaterThan100Percent_WhenCalculateFinalPrice_ThenReturnZero() {
        DiscountPolicy policy = new DiscountPolicy(1, "Free coupon", EVENT_ID, COMPANY_ID);
        policy.addRule(new CouponDiscountRule(100.0, "FREE"));

        assertEquals(0.0, policy.calculateFinalPrice(100.0, 1, "FREE"), 0.001);
    }

    // =========================================================================
    // addRule / clearRules
    // =========================================================================

    @Test
    void GivenNullDiscountRule_WhenAddRule_ThenExceptionThrown() {
        DiscountPolicy policy = new DiscountPolicy(1, "Null rule", EVENT_ID, COMPANY_ID);

        assertThrows(IllegalArgumentException.class, () -> policy.addRule(null));
    }

    @Test
    void GivenValidDiscountRule_WhenAddRule_ThenDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(1, "Has discount", EVENT_ID, COMPANY_ID);
        policy.addRule(new PercentageDiscountRule(10.0, "10% off"));

        // If rule was added, discount > 0
        assertTrue(policy.computeDiscount(ctx(1)) > 0.0);
    }

    @Test
    void GivenDiscountRule_WhenClearRules_ThenNoDiscountApplied() {
        DiscountPolicy policy = new DiscountPolicy(1, "Clear test", EVENT_ID, COMPANY_ID);
        policy.addRule(new PercentageDiscountRule(20.0, "20% off"));

        policy.clearRules();

        assertEquals(0.0, policy.computeDiscount(ctx(1)), 0.001);
    }

    // =========================================================================
    // computeDiscount / conditional check
    // =========================================================================

    @Test
    void GivenConditionalDiscount_WhenQuantityEnough_ThenDiscountPositive() {
        DiscountPolicy policy = new DiscountPolicy(1, "Conditional", EVENT_ID, COMPANY_ID);
        policy.addRule(new QuantityConditionalDiscountRule(2, 10.0));

        assertTrue(policy.computeDiscount(ctx(2)) > 0.0);
    }

    @Test
    void GivenOnlyPercentageDiscount_WhenComputeDiscount_ThenAlwaysPositive() {
        DiscountPolicy policy = new DiscountPolicy(1, "Percentage", EVENT_ID, COMPANY_ID);
        policy.addRule(new PercentageDiscountRule(10.0, "10% off"));

        assertEquals(10.0, policy.computeDiscount(ctx(1)), 0.001);
    }

    @Test
    void GivenConditionalDiscount_WhenQuantityBelowThreshold_ThenDiscountZero() {
        DiscountPolicy policy = new DiscountPolicy(1, "Conditional", EVENT_ID, COMPANY_ID);
        policy.addRule(new QuantityConditionalDiscountRule(5, 20.0));

        assertEquals(0.0, policy.computeDiscount(ctx(2)), 0.001);
    }
}