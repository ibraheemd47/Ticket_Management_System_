package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Discount;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("DISCOUNT")
public class DiscountPolicy extends Policy {

    @Column(name = "is_additive")
    private boolean isAdditive = false;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "root_rule_id")
    private DiscountRule rootRule;

    protected DiscountPolicy() {}

    public DiscountPolicy(int policyId, String description, UUID eventId) {
        super(policyId, description, eventId);
    }

    public void addRule(DiscountRule newRule) {
        if (newRule == null) {
            throw new IllegalArgumentException("rule must not be null");
        }

        List<DiscountRule> current = currentRules();
        current.add(newRule);
        rebuildComposite(current);
    }

    public void setRules(List<DiscountRule> rules, boolean additive) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("rules must not be empty");
        }
        if (rules.stream().anyMatch(rule -> rule == null)) {
            throw new IllegalArgumentException("rules cannot contain null");
        }

        this.isAdditive = additive;
        rebuildComposite(new ArrayList<>(rules));
    }

    public void clearRules() {
        this.rootRule = null;
    }

    public double computeDiscount(DiscountContext context) {
        if (context == null || rootRule == null) {
            return 0.0;
        }

        double discount = rootRule.apply(context);
        return Math.max(0.0, Math.min(100.0, discount));
    }

    public double computeFinalPrice(double originalPrice, DiscountContext context) {
        if (originalPrice < 0) {
            throw new IllegalArgumentException("originalPrice must be non-negative");
        }

        double pct = computeDiscount(context);
        return Math.max(0.0, originalPrice * (1.0 - pct / 100.0));
    }

    public double calculateFinalPrice(double originalPrice, int quantity, String couponCode) {
        return computeFinalPrice(
                originalPrice,
                new DiscountContext(quantity, null, couponCode, getEventId(), null, originalPrice, null, null, null)
        );
    }

    public void setRootRule(DiscountRule rule) {
        this.rootRule = rule;
    }

    public DiscountRule getRootRule() {
        return rootRule;
    }

    public boolean isAdditive() {
        return isAdditive;
    }

    public void setAdditive(boolean flag) {
        this.isAdditive = flag;
    }

    @Override
    public boolean isValid() {
        return rootRule == null || rootRule.isValid();
    }

    private List<DiscountRule> currentRules() {
        if (rootRule instanceof CompositeDiscountRule composite) {
            return new ArrayList<>(composite.getRules());
        }

        List<DiscountRule> list = new ArrayList<>();
        if (rootRule != null) {
            list.add(rootRule);
        }

        return list;
    }

    private void rebuildComposite(List<DiscountRule> rules) {
        if (rules.size() == 1) {
            this.rootRule = rules.get(0);
        } else {
            this.rootRule = isAdditive
                    ? new SumDiscountRule(rules)
                    : new MaxDiscountRule(rules);
        }
    }
}