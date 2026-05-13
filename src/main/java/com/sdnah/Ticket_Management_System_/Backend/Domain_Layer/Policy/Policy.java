package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "policies")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "policy_type")
public abstract class Policy {

    @Id
    @Column(name = "policy_id")
    private int policyId;

    @Column(name = "description")
    private String description;

    @Column(name = "company_id", nullable = false)
    private int companyId;

    /**
     * Nullable: policies can be scoped to a company (null) or to a specific event (non-null).
     */
    @Column(name = "event_id")
    private UUID eventId;

    protected Policy() {
        // JPA
    }

    protected Policy(int policyId, String description, UUID eventId, int companyId) {
        if (policyId <= 0)
            throw new IllegalArgumentException("Policy ID must be positive");
        if (description == null || description.trim().isEmpty())
            throw new IllegalArgumentException("Policy description cannot be empty");
        if (companyId <= 0)
            throw new IllegalArgumentException("Company ID must be positive");
        // eventId may be null for company-scoped policies
        this.policyId    = policyId;
        this.description = description;
        this.eventId     = eventId;
        this.companyId   = companyId;
    }

    public int    getPolicyId()   { return policyId; }
    public String getDescription(){ return description; }
    public UUID   getEventId()    { return eventId; }
    public int    getCompanyId()  { return companyId; }
    

    public abstract boolean isValid();
}