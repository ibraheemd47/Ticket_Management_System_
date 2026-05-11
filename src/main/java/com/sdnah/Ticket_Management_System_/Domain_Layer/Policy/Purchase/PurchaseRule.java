package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

/**
 * Component interface for the Composite purchase-rule tree.
 *
 * Leaf nodes   – MinAgeRule, MinTicketsRule, MaxTicketsRule, NoSingleSeatGapRule
 * Composite nodes – AndRule, OrRule  (arbitrary nesting depth)
 *
 * Adding a new rule type = implement this interface. Zero changes elsewhere.
 */
public interface PurchaseRule {
    RuleResult evaluate(PurchaseContext context);
}