package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;
public abstract class Policy {

    private final int policyId;
    private final String description;
    private final Integer  eventId; // New field to hold the Event ID - null if  not used
    private final int companyId;


    protected Policy(int policyId, String description, Integer  eventId, int companyId) {
         if (policyId <= 0) {
            throw new IllegalArgumentException("Policy ID must be positive");
            
        }
        if (companyId <= 0) {
            throw new IllegalArgumentException("Company ID must be positive");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy description cannot be empty");
        }
        this.policyId = policyId;
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

    public Integer  getEventId() {
        return eventId;
    }
    public int getCompanyId() {
        return companyId;
    }

    public abstract boolean isValid();

    
}