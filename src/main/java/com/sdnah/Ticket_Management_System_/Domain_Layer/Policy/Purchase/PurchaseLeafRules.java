package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

// ─────────────────────────────────────────────────────────────────────────────
// Leaf rules – each enforces exactly one condition.
// To add a new restriction: implement PurchaseRule and handle it here.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Denies purchase when the buyer is younger than the required minimum age.
 * Example: "Tickets can only be purchased by buyers aged 18 and above."
 */
class MinAgeRule implements PurchaseRule {

    private final int minimumAge;

    MinAgeRule(int minimumAge) {
        if (minimumAge < 0) throw new IllegalArgumentException("minimumAge must be non-negative");
        this.minimumAge = minimumAge;
    }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        if (ctx.getBuyerAge() >= minimumAge) return RuleResult.allowed();
        return RuleResult.denied(
            "ניתן לרכוש כרטיסים לאירוע מגיל " + minimumAge + " ומעלה.");
    }

    public int getMinimumAge() { return minimumAge; }
}

/**
 * Denies purchase when the requested quantity exceeds the maximum per order.
 * Example: "You may purchase at most 5 tickets."
 */
class MaxTicketsRule implements PurchaseRule {

    private final int maxTickets;

    MaxTicketsRule(int maxTickets) {
        if (maxTickets <= 0) throw new IllegalArgumentException("maxTickets must be positive");
        this.maxTickets = maxTickets;
    }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        if (ctx.getTicketQuantity() <= maxTickets) return RuleResult.allowed();
        return RuleResult.denied(
            "ניתן לקנות לכל היותר " + maxTickets + " כרטיסים.");
    }

    public int getMaxTickets() { return maxTickets; }
}

/**
 * Denies purchase when the requested quantity is below the minimum per order.
 * Example: "You must purchase at least 2 tickets."
 */
class MinTicketsRule implements PurchaseRule {

    private final int minTickets;

    MinTicketsRule(int minTickets) {
        if (minTickets <= 0) throw new IllegalArgumentException("minTickets must be positive");
        this.minTickets = minTickets;
    }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        if (ctx.getTicketQuantity() >= minTickets) return RuleResult.allowed();
        return RuleResult.denied(
            "ניתן לקנות לכל הפחות " + minTickets + " כרטיסים.");
    }

    public int getMinTickets() { return minTickets; }
}

/**
 * Denies purchase if it would leave a single isolated seat in a row.
 * Used to prevent the anti-social "single gap" seating situation.
 */
class NoSingleSeatGapRule implements PurchaseRule {

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        if (!ctx.isCreatesSingleSeatGap()) return RuleResult.allowed();
        return RuleResult.denied(
            "הרכישה תיצור מושב בודד מבודד – אנא בחר מושב שונה.");
    }
}