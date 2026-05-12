package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

/**
 * Abstract composite rule — stored in the same purchase_rules table.
 * Children linked via parent_rule_id (self-referential @OneToMany).
 * JPA loads the entire tree automatically with EAGER fetch.
 *
 * AndRule – ALL children must pass.
 * OrRule  – ANY child must pass.
 */
@Entity
@DiscriminatorValue("COMPOSITE")
public abstract class CompositePurchaseRule extends PurchaseRule {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "parent_rule_id")
    protected List<PurchaseRule> rules = new ArrayList<>();

    protected CompositePurchaseRule() {}

    protected CompositePurchaseRule(List<PurchaseRule> rules) {
        if (rules == null || rules.isEmpty())
            throw new IllegalArgumentException("CompositePurchaseRule requires at least one child");
        this.rules = new ArrayList<>(rules);
    }

    protected CompositePurchaseRule(PurchaseRule a, PurchaseRule b) {
        this(List.of(a, b));
    }

    public List<PurchaseRule> getRules() { return List.copyOf(rules); }
}