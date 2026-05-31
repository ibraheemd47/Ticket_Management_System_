package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CouponDiscountRule — Domain Unit Tests")
class CouponDiscountRuleTest {

    // =========================================================================
    // Validation in constructor
    // =========================================================================

    @Test
    void GivenNegativePercentage_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CouponDiscountRule(-1.0, "SAVE10"));
    }

    @Test
    void GivenPercentageOver100_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CouponDiscountRule(101.0, "SAVE10"));
    }

    @Test
    void GivenNullCouponCode_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CouponDiscountRule(10.0, null));
    }

    @Test
    void GivenBlankCouponCode_WhenConstructed_ThenThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CouponDiscountRule(10.0, "   "));
    }

    @Test
    void GivenValidArgs_WhenConstructed_ThenCouponCodeIsTrimmed() {
        CouponDiscountRule rule = new CouponDiscountRule(10.0, "  SAVE10  ");

        assertEquals("SAVE10", rule.getCouponCode());
    }

    // =========================================================================
    // apply() — code matching
    // =========================================================================

    @Test
    void GivenCorrectCoupon_WhenApply_ThenReturnPercentage() {
        CouponDiscountRule rule = new CouponDiscountRule(15.0, "SAVE15");

        DiscountContext ctx = new DiscountContext(1, LocalDateTime.now(), "SAVE15");

        assertEquals(15.0, rule.apply(ctx), 0.001);
    }

    @Test
    void GivenCouponWithDifferentCase_WhenApply_ThenStillMatches() {
        CouponDiscountRule rule = new CouponDiscountRule(15.0, "SAVE15");

        DiscountContext ctx = new DiscountContext(1, LocalDateTime.now(), "save15");

        assertEquals(15.0, rule.apply(ctx), 0.001);
    }

    @Test
    void GivenCouponWithWhitespace_WhenApply_ThenStillMatches() {
        CouponDiscountRule rule = new CouponDiscountRule(15.0, "SAVE15");

        DiscountContext ctx = new DiscountContext(1, LocalDateTime.now(), "  SAVE15  ");

        assertEquals(15.0, rule.apply(ctx), 0.001);
    }

    @Test
    void GivenWrongCoupon_WhenApply_ThenReturnZero() {
        CouponDiscountRule rule = new CouponDiscountRule(15.0, "SAVE15");

        DiscountContext ctx = new DiscountContext(1, LocalDateTime.now(), "OTHER");

        assertEquals(0.0, rule.apply(ctx), 0.001);
    }

    @Test
    void GivenNoCouponInContext_WhenApply_ThenReturnZero() {
        CouponDiscountRule rule = new CouponDiscountRule(15.0, "SAVE15");

        DiscountContext ctx = new DiscountContext(1, LocalDateTime.now());

        assertEquals(0.0, rule.apply(ctx), 0.001);
    }

    // =========================================================================
    // apply() — expiry
    // =========================================================================

    @Test
    void GivenExpiredCoupon_WhenApplied_ThenReturnZero() {
        LocalDateTime expiry = LocalDateTime.of(2026, 1, 1, 0, 0);
        CouponDiscountRule rule = new CouponDiscountRule(15.0, "SAVE15", expiry);

        DiscountContext ctx = new DiscountContext(1, expiry.plusDays(1), "SAVE15");

        assertEquals(0.0, rule.apply(ctx), 0.001);
    }

    @Test
    void GivenCouponBeforeExpiry_WhenApplied_ThenReturnPercentage() {
        LocalDateTime expiry = LocalDateTime.of(2026, 12, 31, 23, 59);
        CouponDiscountRule rule = new CouponDiscountRule(15.0, "SAVE15", expiry);

        DiscountContext ctx = new DiscountContext(1, expiry.minusDays(1), "SAVE15");

        assertEquals(15.0, rule.apply(ctx), 0.001);
    }

    @Test
    void GivenNullExpiry_WhenApplied_ThenNeverExpires() {
        CouponDiscountRule rule = new CouponDiscountRule(15.0, "SAVE15", null);

        DiscountContext ctx = new DiscountContext(1, LocalDateTime.of(2099, 1, 1, 0, 0), "SAVE15");

        assertEquals(15.0, rule.apply(ctx), 0.001);
    }

    // =========================================================================
    // describe()
    // =========================================================================

    @Test
    void GivenCouponWithoutExpiry_WhenDescribe_ThenNoExpiryMention() {
        CouponDiscountRule rule = new CouponDiscountRule(20.0, "SAVE20");

        String desc = rule.describe();
        assertTrue(desc.contains("20"));
        assertTrue(desc.contains("SAVE20"));
        assertFalse(desc.contains("valid until"));
    }

    @Test
    void GivenCouponWithExpiry_WhenDescribe_ThenContainsExpiry() {
        LocalDateTime expiry = LocalDateTime.of(2026, 12, 31, 23, 59);
        CouponDiscountRule rule = new CouponDiscountRule(20.0, "SAVE20", expiry);

        String desc = rule.describe();
        assertTrue(desc.contains("valid until"));
        assertTrue(desc.contains("2026"));
    }

    // =========================================================================
    // Getters
    // =========================================================================

    @Test
    void GivenCoupon_WhenGetters_ThenReturnConstructedValues() {
        LocalDateTime expiry = LocalDateTime.of(2026, 6, 1, 0, 0);
        CouponDiscountRule rule = new CouponDiscountRule(25.0, "SAVE25", expiry);

        assertEquals(25.0, rule.getPercentage());
        assertEquals("SAVE25", rule.getCouponCode());
        assertEquals(expiry, rule.getExpiry());
    }
}