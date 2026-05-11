package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

import jakarta.persistence.*;

/**
 * Abstract base Entity for all purchase rules (leaf and composite).
 * Stored in purchase_rules table using SINGLE_TABLE inheritance.
 * JPA handles the entire tree automatically via parent_rule_id.
 *
 * Hierarchy:
 *   PurchaseRule
 *   ├── MinAgeRule
 *   ├── MinTicketsRule
 *   ├── MaxTicketsRule
 *   └── CompositePurchaseRule (abstract)
 *       ├── AndRule
 *       └── OrRule
 */
@Entity
@Table(name = "purchase_rules")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rule_type")
public abstract class PurchaseRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    public Long getId() { return id; }

    public abstract RuleResult evaluate(PurchaseContext context);
    public abstract String describe();
}