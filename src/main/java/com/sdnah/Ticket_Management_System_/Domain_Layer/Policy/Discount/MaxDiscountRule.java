package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount;

import java.util.List;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("MAX")
public class MaxDiscountRule extends CompositeDiscountRule {

    protected MaxDiscountRule() {}

    public MaxDiscountRule(List<DiscountRule> rules) { super(rules); }
    public MaxDiscountRule(DiscountRule a, DiscountRule b) { super(a, b); }

    @Override
    public double apply(DiscountContext context) {
        return rules.stream()
                .mapToDouble(r -> r.apply(context))
                .max()
                .orElse(0.0);
    }

    @Override
    public String describe() {
        return "Best of: [" + rules.stream()
                .map(DiscountRule::describe)
                .reduce((a, b) -> a + " | " + b)
                .orElse("") + "]";
    }
}