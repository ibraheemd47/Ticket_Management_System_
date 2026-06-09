package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy;

import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "policies")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "policy_type")
public abstract class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "policy_seq")
    @SequenceGenerator(name = "policy_seq", sequenceName = "policies_policy_id_seq", allocationSize = 1)
    @Column(name = "policy_id")
    private int policyId;

    @Column(name = "description")
    private String description;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    /**
     * Nullable: policies can be scoped to a company (null) or to a specific event
     * (non-null).
     */
    @Column(name = "event_id")
    private UUID eventId;

    public enum CombineMode {
        AND, OR
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "combine_mode")
    private CombineMode combineMode = CombineMode.AND;

    protected Policy() {
        // JPA
    }

    protected Policy(String description, UUID eventId, UUID companyId) {
        if (description == null || description.trim().isEmpty())
            throw new IllegalArgumentException("Policy description cannot be empty");
        if (companyId == null)
            throw new IllegalArgumentException("Company ID must be positive");
        // eventId may be null for company-scoped policies
        this.description = description;
        this.eventId = eventId;
        this.companyId = companyId;
    }

    public int getPolicyId() {
        return policyId;
    }

    public String getDescription() {
        return description;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public abstract boolean isValid();
}