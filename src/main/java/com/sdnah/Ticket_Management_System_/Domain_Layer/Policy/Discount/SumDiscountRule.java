package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount;

import java.util.List;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("SUM")
public class SumDiscountRule extends CompositeDiscountRule {

    protected SumDiscountRule() {}

    public SumDiscountRule(List<DiscountRule> rules) { super(rules); }
    public SumDiscountRule(DiscountRule a, DiscountRule b) { super(a, b); }

    @Override
    public double apply(DiscountContext context) {
        double total = rules.stream()
                .mapToDouble(r -> r.apply(context))
                .sum();
        return Math.min(total, 100.0);
    }

    @Override
    public String describe() {
        return "Sum of: [" + rules.stream()
                .map(DiscountRule::describe)
                .reduce((a, b) -> a + " + " + b)
                .orElse("") + "]";
    }
}