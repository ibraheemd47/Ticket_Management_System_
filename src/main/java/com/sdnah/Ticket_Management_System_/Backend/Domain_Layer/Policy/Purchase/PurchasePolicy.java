package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Policy;

import jakarta.persistence.*;
//Implement purchase policy base abstraction
@Entity
@DiscriminatorValue("PURCHASE")
public class PurchasePolicy extends Policy {

    public enum Operator { AND, OR }

    /**
     * How multiple rules are combined.
     * AND = all conditions must pass (default).
     * OR  = at least one condition must pass.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_operator")
    private Operator operator = Operator.AND;

    /**
     * null = no restrictions (all purchases allowed).
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "root_purchase_rule_id")
    private PurchaseRule rootRule;

    protected PurchasePolicy() {}

    public PurchasePolicy(int policyId, String description, UUID eventId, int companyId) {
        super(policyId, description, eventId, companyId);
    }

    // ── Core API ───────────────────────────────────────────────────────────────

    public RuleResult validate(PurchaseContext context) {
        if (context == null) return RuleResult.denied("Missing purchase context.");
        if (rootRule == null) return RuleResult.allowed();
        return rootRule.evaluate(context);
    }

    public boolean validatePurchase(int quantity, int buyerAge, boolean createsSingleGap) {
        return validate(new PurchaseContext(buyerAge, quantity, createsSingleGap, true, null))
                .isAllowed();
    }

    public boolean validatePurchase(int quantity, boolean createsSingleGap) {
        return validate(new PurchaseContext(0, quantity, createsSingleGap, true, null))
                .isAllowed();
    }

    // ── UC II.4.3 ─────────────────────────────────────────────────────────────

    public void addRule(PurchaseRule newRule) {
        if (newRule == null) throw new IllegalArgumentException("rule must not be null");
        List<PurchaseRule> current = currentRules();
        current.add(newRule);
        rebuildComposite(current);
    }

    public void setRules(List<PurchaseRule> rules, Operator operator) {
        if (rules == null || rules.isEmpty())
            throw new IllegalArgumentException("rules must not be empty");
        this.operator = operator != null ? operator : Operator.AND;
        rebuildComposite(new ArrayList<>(rules));
    }

    public void clearRules() {
        this.rootRule = null;
    }

    // ── Getters / setters ──────────────────────────────────────────────────────

    public void         setRootRule(PurchaseRule rule) { this.rootRule = rule; }
    public PurchaseRule getRootRule()                   { return rootRule; }
    public Operator     getOperator()                   { return operator; }
    public void         setOperator(Operator op)        { this.operator = op; }

    @Override
    public boolean isValid() { return true; }

    // ── Private helpers ────────────────────────────────────────────────────────

    private List<PurchaseRule> currentRules() {
        if (rootRule instanceof CompositePurchaseRule composite) {
            return new ArrayList<>(composite.getRules());
        }
        List<PurchaseRule> list = new ArrayList<>();
        if (rootRule != null) list.add(rootRule);
        return list;
    }

    private void rebuildComposite(List<PurchaseRule> rules) {
        if (rules.size() == 1) {
            this.rootRule = rules.get(0);
        } else {
            this.rootRule = operator == Operator.OR
                    ? new OrRule(rules)
                    : new AndRule(rules);
        }
    }
}