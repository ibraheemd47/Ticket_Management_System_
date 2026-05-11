package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite rule – OR semantics.
 * Purchase is allowed when ANY child rule passes.
 * On full failure, combines all denial messages for the UI.
 *
 * Example: "qty ≤ 2 OR qty ≥ 100"
 */
public class OrRule implements PurchaseRule {

    private final List<PurchaseRule> rules;

    public OrRule(List<PurchaseRule> rules) {
        if (rules == null || rules.isEmpty())
            throw new IllegalArgumentException("OrRule requires at least one child rule");
        this.rules = new ArrayList<>(rules);
    }

    public OrRule(PurchaseRule a, PurchaseRule b) {
        this(List.of(a, b));
    }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        List<String> denials = new ArrayList<>();
        for (PurchaseRule rule : rules) {
            RuleResult result = rule.evaluate(ctx);
            if (result.isAllowed()) return RuleResult.allowed();
            denials.add(result.getMessage());
        }
        return RuleResult.denied(
            "אף אחד מהתנאים לא התקיים: [" +
            String.join("] או [", denials) + "]");
    }

    public List<PurchaseRule> getRules() { return List.copyOf(rules); }
}