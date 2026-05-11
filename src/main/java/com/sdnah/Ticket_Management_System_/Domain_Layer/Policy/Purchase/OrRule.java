package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("OR")
public class OrRule extends CompositePurchaseRule {

    protected OrRule() {}

    public OrRule(List<PurchaseRule> rules) { super(rules); }
    public OrRule(PurchaseRule a, PurchaseRule b) { super(a, b); }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        List<String> denials = new ArrayList<>();
        for (PurchaseRule rule : rules) {
            RuleResult result = rule.evaluate(ctx);
            if (result.isAllowed()) return RuleResult.allowed();
            denials.add(result.getMessage());
        }
        return RuleResult.denied(
            "None of the required conditions were met: ["
            + String.join("] OR [", denials) + "]");
    }

    @Override
    public String describe() {
        return "Any of: [" + rules.stream()
                .map(PurchaseRule::describe)
                .reduce((a, b) -> a + " OR " + b)
                .orElse("") + "]";
    }
}