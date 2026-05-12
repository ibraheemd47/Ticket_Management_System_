package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("MIN_AGE")
public class MinAgeRule extends PurchaseRule {

    @Column(name = "minimum_age")
    private int minimumAge;

    protected MinAgeRule() {}

    public MinAgeRule(int minimumAge) {
        if (minimumAge < 0) throw new IllegalArgumentException("minimumAge must be non-negative");
        this.minimumAge = minimumAge;
    }

    @Override
    public RuleResult evaluate(PurchaseContext ctx) {
        if (ctx.getBuyerAge() >= minimumAge) return RuleResult.allowed();
        return RuleResult.denied(
            "Tickets for this event can only be purchased by buyers aged "
            + minimumAge + " and above.");
    }

    @Override
    public String describe() {
        return "Minimum age: " + minimumAge;
    }

    public int getMinimumAge() { return minimumAge; }
}