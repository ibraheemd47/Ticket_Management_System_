package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite rule – AND semantics.
 * Purchase is allowed only when ALL child rules pass.
 * Short-circuits on the first failure (returns that rule's denial message).
 *
 * Example: "age ≥ 18 AND (qty ≤ 2 OR qty ≥ 100)"
 */
public class AndRule implements PurchaseRule {

    private final List<PurchaseRule> rules;

    public AndRule(List<PurchaseRule> rules) {
        if (rules == null || rules.isEmpty())
            throw new IllegalArgumentException("AndRule requires at least one child rule");
        this.rules = new ArrayList<>(rules);
    }

    public AndRule(PurchaseRule a, PurchaseRule b) {
        this(List.of(a, b));
    }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        for (PurchaseRule rule : rules) {
            RuleResult result = rule.evaluate(ctx);
            if (!result.isAllowed()) return result; // short-circuit
        }
        return RuleResult.allowed();
    }

    public List<PurchaseRule> getRules() { return List.copyOf(rules); }
}