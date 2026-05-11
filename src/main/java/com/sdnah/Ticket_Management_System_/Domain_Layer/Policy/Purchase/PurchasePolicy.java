package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Policy;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("PURCHASE")
public class PurchasePolicy extends Policy {

    @Column(name = "rule_tree_json", columnDefinition = "TEXT")
    private String ruleTreeJson;

    @Transient
    private PurchaseRule rootRule = PurchaseRuleFactory.noRestrictions();

    protected PurchasePolicy() {
        // JPA
    }

    public PurchasePolicy(int policyId, String description, UUID eventId) {
        super(policyId, description, eventId);
    }

    public RuleResult validate(PurchaseContext context) {
        if (context == null) {
            return RuleResult.denied("Missing purchase context.");
        }
        if (rootRule == null) {
            return RuleResult.allowed();
        }
        return rootRule.evaluate(context);
    }

    public boolean validatePurchase(int quantity, int buyerAge, boolean createsSingleGap) {
        PurchaseContext ctx = new PurchaseContext(
                buyerAge,
                quantity,
                createsSingleGap,
                true,
                null
        );
        return validate(ctx).isAllowed();
    }

    // Backward compatibility only
    public boolean validatePurchase(int quantity, boolean createsSingleGap) {
        PurchaseContext ctx = new PurchaseContext(
                0,
                quantity,
                createsSingleGap,
                true,
                null
        );
        return validate(ctx).isAllowed();
    }

    public void setRootRule(PurchaseRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("rootRule must not be null");
        }
        this.rootRule = rule;
    }

    public PurchaseRule getRootRule() {
        return rootRule;
    }

    public String getRuleTreeJson() {
        return ruleTreeJson;
    }

    public void setRuleTreeJson(String json) {
        this.ruleTreeJson = json;
    }

    @Override
    public boolean isValid() {
        return rootRule != null;
    }
}