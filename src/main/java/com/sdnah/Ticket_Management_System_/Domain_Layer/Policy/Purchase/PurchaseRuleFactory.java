package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

import java.util.List;


/**
 * Factory for building purchase rule trees.
 *
 * Application/REST layers call this factory instead of constructing
 * rule classes directly, keeping the domain model internal.
 *
 * Usage:
 *   PurchaseRule rule = PurchaseRuleFactory.and(
 *       PurchaseRuleFactory.minAge(18),
 *       PurchaseRuleFactory.or(
 *           PurchaseRuleFactory.maxTickets(2),
 *           PurchaseRuleFactory.minTickets(100)
 *       )
 *   );
 */
public final class PurchaseRuleFactory {

    private PurchaseRuleFactory() {}

    public static PurchaseRule minAge(int age)          { return new MinAgeRule(age); }
    public static PurchaseRule maxTickets(int max)       { return new MaxTicketsRule(max); }
    public static PurchaseRule minTickets(int min)       { return new MinTicketsRule(min); }
    public static PurchaseRule noSingleSeatGap()         { return new NoSingleSeatGapRule(); }

    public static PurchaseRule and(PurchaseRule a, PurchaseRule b)    { return new AndRule(a, b); }
    public static PurchaseRule and(List<PurchaseRule> rules)          { return new AndRule(rules); }

    public static PurchaseRule or(PurchaseRule a, PurchaseRule b)     { return new OrRule(a, b); }
    public static PurchaseRule or(List<PurchaseRule> rules)           { return new OrRule(rules); }

    /** No restrictions at all – used as the default policy. */
    public static PurchaseRule noRestrictions() { return ctx -> RuleResult.allowed(); }
}