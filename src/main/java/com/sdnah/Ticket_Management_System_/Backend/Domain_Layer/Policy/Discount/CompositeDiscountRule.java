package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("COMPOSITE")
public abstract class CompositeDiscountRule extends DiscountRule {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "parent_rule_id")
    protected List<DiscountRule> rules = new ArrayList<>();

    protected CompositeDiscountRule() {}

    protected CompositeDiscountRule(List<DiscountRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("CompositeDiscountRule requires at least one child rule");
        }
        if (rules.stream().anyMatch(r -> r == null)) {
            throw new IllegalArgumentException("CompositeDiscountRule cannot contain null rules");
        }

        this.rules = new ArrayList<>(rules);
    }

    protected CompositeDiscountRule(DiscountRule a, DiscountRule b) {
        this(List.of(a, b));
    }

    public List<DiscountRule> getRules() {
        return List.copyOf(rules);
    }

    @Override
    public boolean isValid() {
        return rules != null
                && !rules.isEmpty()
                && rules.stream().allMatch(rule -> rule != null && rule.isValid());
    }
}