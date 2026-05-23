package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DateRangeDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.MaxDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.SumDiscountRule;

/**
 * Unit tests for the discount rule hierarchy: leaf rules + composite (Sum/Max)
 * + the DiscountContext value object that's threaded through them. Focuses on
 * paths the existing DiscountPolicyTest doesn't exercise.
 */
class DiscountRulesTest {

    // ── DiscountContext ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DiscountContext: full ctor stores every field; hasCoupon reflects non-blank code")
    void discountContext_FullCtor() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime t = LocalDateTime.of(2026, 6, 1, 18, 0);

        DiscountContext c = new DiscountContext(3, t, "SAVE10", 200.0, eventId);

        assertThat(c.getTicketQuantity()).isEqualTo(3);
        assertThat(c.getPurchaseTime()).isEqualTo(t);
        assertThat(c.getCouponCode()).isEqualTo("SAVE10");
        assertThat(c.getOriginalPrice()).isEqualTo(200.0);
        assertThat(c.getEventId()).isEqualTo(eventId);
        assertThat(c.hasCoupon()).isTrue();
    }

    @Test
    @DisplayName("DiscountContext: null purchaseTime defaults to now")
    void discountContext_NullTime_DefaultsNow() {
        DiscountContext c = new DiscountContext(1, null);
        assertThat(c.getPurchaseTime()).isNotNull();
    }

    @Test
    @DisplayName("DiscountContext: hasCoupon false for null / blank / not-set")
    void discountContext_HasCoupon() {
        assertThat(new DiscountContext(1, LocalDateTime.now()).hasCoupon()).isFalse();
        assertThat(new DiscountContext(1, LocalDateTime.now(), "  ").hasCoupon()).isFalse();
        assertThat(new DiscountContext(1, LocalDateTime.now(), null).hasCoupon()).isFalse();
        assertThat(new DiscountContext(1, LocalDateTime.now(), "X").hasCoupon()).isTrue();
    }

    @Test
    @DisplayName("DiscountContext rejects non-positive quantity and negative price")
    void discountContext_RejectsInvalid() {
        assertThatThrownBy(() -> new DiscountContext(0, LocalDateTime.now(), null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiscountContext(-1, LocalDateTime.now(), null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiscountContext(1, LocalDateTime.now(), null, -0.01, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── DateRangeDiscountRule ──────────────────────────────────────────────

    @Test
    @DisplayName("DateRangeDiscountRule: percentage applies inside the window, zero outside")
    void dateRange_AppliesInsideWindow() {
        LocalDateTime from  = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime until = LocalDateTime.of(2026, 6, 30, 23, 59);
        DateRangeDiscountRule rule = new DateRangeDiscountRule(20.0, from, until);

        DiscountContext inside  = new DiscountContext(1, LocalDateTime.of(2026, 6, 15, 12, 0));
        DiscountContext beforeT = new DiscountContext(1, LocalDateTime.of(2026, 5, 30, 12, 0));
        DiscountContext after   = new DiscountContext(1, LocalDateTime.of(2026, 7, 1, 12, 0));

        assertThat(rule.apply(inside)).isEqualTo(20.0);
        assertThat(rule.apply(beforeT)).isEqualTo(0.0);
        assertThat(rule.apply(after)).isEqualTo(0.0);

        // Describe covers the from-to branch
        assertThat(rule.describe()).contains("20.0").contains(from.toString()).contains(until.toString());
        assertThat(rule.getPercentage()).isEqualTo(20.0);
        assertThat(rule.getFrom()).isEqualTo(from);
        assertThat(rule.getUntil()).isEqualTo(until);
    }

    @Test
    @DisplayName("DateRangeDiscountRule.before(...) treats range as 'up to'")
    void dateRange_BeforeOnly() {
        LocalDateTime deadline = LocalDateTime.of(2026, 6, 1, 0, 0);
        DateRangeDiscountRule rule = DateRangeDiscountRule.before(15.0, deadline);

        assertThat(rule.apply(new DiscountContext(1, deadline.minusHours(1)))).isEqualTo(15.0);
        assertThat(rule.apply(new DiscountContext(1, deadline.plusHours(1)))).isEqualTo(0.0);
        assertThat(rule.describe()).contains("up to");
        assertThat(rule.getFrom()).isNull();
    }

    @Test
    @DisplayName("DateRangeDiscountRule.after(...) treats range as 'from'")
    void dateRange_AfterOnly() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 0, 0);
        DateRangeDiscountRule rule = DateRangeDiscountRule.after(25.0, start);

        assertThat(rule.apply(new DiscountContext(1, start.plusHours(1)))).isEqualTo(25.0);
        assertThat(rule.apply(new DiscountContext(1, start.minusHours(1)))).isEqualTo(0.0);
        assertThat(rule.describe()).contains("from");
        assertThat(rule.getUntil()).isNull();
    }

    @Test
    @DisplayName("DateRangeDiscountRule rejects invalid percentage or inverted range")
    void dateRange_RejectsInvalid() {
        assertThatThrownBy(() -> new DateRangeDiscountRule(-1, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DateRangeDiscountRule(101, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        LocalDateTime a = LocalDateTime.of(2026, 6, 5, 0, 0);
        LocalDateTime b = LocalDateTime.of(2026, 6, 1, 0, 0);
        assertThatThrownBy(() -> new DateRangeDiscountRule(10, a, b))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── SumDiscountRule (composite — additive) ─────────────────────────────

    @Test
    @DisplayName("SumDiscountRule: returns the sum of children, capped at 100")
    void sum_AddsChildrenCappedAt100() {
        DiscountRule a = new PercentageDiscountRule(30, "a");
        DiscountRule b = new PercentageDiscountRule(40, "b");
        DiscountRule c = new PercentageDiscountRule(50, "c");

        SumDiscountRule sum = new SumDiscountRule(List.of(a, b, c));
        DiscountContext ctx = new DiscountContext(1, LocalDateTime.now());

        assertThat(sum.apply(ctx)).isEqualTo(100.0); // 30+40+50 = 120 capped
        assertThat(sum.describe()).contains("Sum of");
        assertThat(sum.getRules()).hasSize(3);
        assertThat(sum.isValid()).isTrue();
    }

    @Test
    @DisplayName("SumDiscountRule: two-arg ctor stores both children")
    void sum_TwoArgCtor() {
        SumDiscountRule sum = new SumDiscountRule(
                new PercentageDiscountRule(10, "x"),
                new PercentageDiscountRule(20, "y"));

        DiscountContext ctx = new DiscountContext(1, LocalDateTime.now());
        assertThat(sum.apply(ctx)).isEqualTo(30.0);
        assertThat(sum.getRules()).hasSize(2);
    }

    // ── MaxDiscountRule (composite — "best of") ────────────────────────────

    @Test
    @DisplayName("MaxDiscountRule: returns the largest of its children")
    void max_PicksLargest() {
        DiscountRule a = new PercentageDiscountRule(10, "a");
        DiscountRule b = new PercentageDiscountRule(25, "b");
        DiscountRule c = new PercentageDiscountRule(15, "c");

        MaxDiscountRule max = new MaxDiscountRule(List.of(a, b, c));
        DiscountContext ctx = new DiscountContext(1, LocalDateTime.now());

        assertThat(max.apply(ctx)).isEqualTo(25.0);
        assertThat(max.describe()).contains("Best of");
        assertThat(max.isValid()).isTrue();
    }

    @Test
    @DisplayName("MaxDiscountRule: two-arg ctor + getRules expose an unmodifiable view")
    void max_TwoArgCtor() {
        MaxDiscountRule max = new MaxDiscountRule(
                new PercentageDiscountRule(10, "x"),
                new QuantityConditionalDiscountRule(5, 20));

        // For a 1-ticket context the conditional yields 0, percentage yields 10.
        DiscountContext ctx = new DiscountContext(1, LocalDateTime.now());
        assertThat(max.apply(ctx)).isEqualTo(10.0);
        assertThat(max.getRules()).hasSize(2);

        assertThatThrownBy(() -> max.getRules().add(new PercentageDiscountRule(1, "z")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ── Composite invariants (via SumDiscountRule) ─────────────────────────

    @Test
    @DisplayName("CompositeDiscountRule rejects empty / null child lists and null children")
    void composite_RejectsInvalidChildren() {
        assertThatThrownBy(() -> new SumDiscountRule((List<DiscountRule>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SumDiscountRule(List.of()))
                .isInstanceOf(IllegalArgumentException.class);

        DiscountRule[] withNull = { new PercentageDiscountRule(10, "x"), null };
        assertThatThrownBy(() ->
                new SumDiscountRule(java.util.Arrays.asList(withNull)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── CouponDiscountRule (extra paths beyond DiscountPolicyTest) ─────────

    @Test
    @DisplayName("CouponDiscountRule applies its percent when the code matches; zero otherwise")
    void coupon_CodeMatchVsMismatch() {
        CouponDiscountRule coupon = new CouponDiscountRule(15, "SAVE15");

        DiscountContext ok  = new DiscountContext(1, LocalDateTime.now(), "SAVE15");
        DiscountContext bad = new DiscountContext(1, LocalDateTime.now(), "OTHER");
        DiscountContext no  = new DiscountContext(1, LocalDateTime.now());

        assertThat(coupon.apply(ok)).isEqualTo(15.0);
        assertThat(coupon.apply(bad)).isEqualTo(0.0);
        assertThat(coupon.apply(no)).isEqualTo(0.0);
        assertThat(coupon.describe()).contains("SAVE15");
    }
}
