package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PercentageDiscountRule — Domain Unit Tests")
class PercentageDiscountRuleTest {

    private DiscountContext ctx() {
        return new DiscountContext(1, LocalDateTime.now());
    }

    @Test
    void GivenValidPercentage_WhenConstructed_ThenStored() {
        PercentageDiscountRule rule = new PercentageDiscountRule(10.0, "10% off");

        assertEquals(10.0, rule.getPercentage());
        assertEquals("10% off", rule.getDescription());
    }

    @Test
    void GivenNullDescription_WhenConstructed_ThenDefaultDescriptionUsed() {
        PercentageDiscountRule rule = new PercentageDiscountRule(15.0, null);

        assertNotNull(rule.getDescription());
        assertTrue(rule.getDescription().contains("15"));
    }

    @Test
    void GivenNegativePercentage_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new PercentageDiscountRule(-1.0, "neg"));
    }

    @Test
    void GivenPercentageOver100_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new PercentageDiscountRule(101.0, "too high"));
    }

    @Test
    void GivenZeroPercentage_WhenConstructed_ThenAccepted() {
        PercentageDiscountRule rule = new PercentageDiscountRule(0.0, "no discount");

        assertEquals(0.0, rule.apply(ctx()), 0.001);
    }

    @Test
    void GivenPercentage100_WhenConstructed_ThenAccepted() {
        PercentageDiscountRule rule = new PercentageDiscountRule(100.0, "free");

        assertEquals(100.0, rule.apply(ctx()), 0.001);
    }

    @Test
    void GivenPercentageRule_WhenApply_ThenAlwaysReturnsSameValueRegardlessOfContext() {
        PercentageDiscountRule rule = new PercentageDiscountRule(25.0, "25% off");

        DiscountContext c1 = new DiscountContext(1, LocalDateTime.now());
        DiscountContext c2 = new DiscountContext(10, LocalDateTime.now(), "ANY_COUPON");

        assertEquals(25.0, rule.apply(c1), 0.001);
        assertEquals(25.0, rule.apply(c2), 0.001);
    }

    @Test
    void GivenPercentageRule_WhenDescribe_ThenReturnsDescription() {
        PercentageDiscountRule rule = new PercentageDiscountRule(20.0, "Summer sale");

        assertEquals("Summer sale", rule.describe());
    }
}