package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase;

import java.util.List;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("AND")
public class AndRule extends CompositePurchaseRule {

    protected AndRule() {}

    public AndRule(List<PurchaseRule> rules) { super(rules); }
    public AndRule(PurchaseRule a, PurchaseRule b) { super(a, b); }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        for (PurchaseRule rule : rules) {
            RuleResult result = rule.evaluate(ctx);
            if (!result.isAllowed()) return result; // short-circuit on first failure
        }
        return RuleResult.allowed();
    }

    @Override
    public String describe() {
        return "All of: [" + rules.stream()
                .map(PurchaseRule::describe)
                .reduce((a, b) -> a + " AND " + b)
                .orElse("") + "]";
    }
}