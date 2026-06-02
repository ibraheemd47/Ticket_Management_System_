package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QuantityConditionalDiscountRule — Domain Unit Tests")
class QuantityConditionalDiscountRuleTest {

    private DiscountContext ctx(int qty) {
        return new DiscountContext(qty, LocalDateTime.now());
    }

    // =========================================================================
    // Validation
    // =========================================================================

    @Test
    void GivenZeroMinTickets_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new QuantityConditionalDiscountRule(0, 10.0));
    }

    @Test
    void GivenNegativeMinTickets_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new QuantityConditionalDiscountRule(-1, 10.0));
    }

    @Test
    void GivenNegativePercentage_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new QuantityConditionalDiscountRule(3, -5.0));
    }

    @Test
    void GivenPercentageOver100_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new QuantityConditionalDiscountRule(3, 101.0));
    }

    // =========================================================================
    // apply() — boundary conditions
    // =========================================================================

    @Test
    void GivenQuantityExactlyAtThreshold_WhenApply_ThenDiscountApplied() {
        QuantityConditionalDiscountRule rule = new QuantityConditionalDiscountRule(3, 15.0);

        assertEquals(15.0, rule.apply(ctx(3)), 0.001);
    }

    @Test
    void GivenQuantityAboveThreshold_WhenApply_ThenDiscountApplied() {
        QuantityConditionalDiscountRule rule = new QuantityConditionalDiscountRule(3, 15.0);

        assertEquals(15.0, rule.apply(ctx(10)), 0.001);
    }

    @Test
    void GivenQuantityBelowThreshold_WhenApply_ThenZeroDiscount() {
        QuantityConditionalDiscountRule rule = new QuantityConditionalDiscountRule(3, 15.0);

        assertEquals(0.0, rule.apply(ctx(2)), 0.001);
    }

    @Test
    void GivenQuantityOneBelowThreshold_WhenApply_ThenZeroDiscount() {
        QuantityConditionalDiscountRule rule = new QuantityConditionalDiscountRule(5, 25.0);

        assertEquals(0.0, rule.apply(ctx(4)), 0.001);
    }

    // =========================================================================
    // describe() + getters
    // =========================================================================

    @Test
    void GivenRule_WhenDescribe_ThenContainsThresholdAndPercentage() {
        QuantityConditionalDiscountRule rule = new QuantityConditionalDiscountRule(5, 20.0);

        String desc = rule.describe();
        assertTrue(desc.contains("5"));
        assertTrue(desc.contains("20"));
    }

    @Test
    void GivenRule_WhenGetters_ThenReturnConstructedValues() {
        QuantityConditionalDiscountRule rule = new QuantityConditionalDiscountRule(4, 15.0);

        assertEquals(4, rule.getMinTickets());
        assertEquals(15.0, rule.getPercentage());
    }
}