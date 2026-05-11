package com.sdnah.Ticket_Management_System_.Backend.DTOs.Policy;

import java.util.UUID;

public abstract class PolicyDTO {
    private int policyId;
    private String description;
    private UUID eventId;

    public PolicyDTO() {}

    public PolicyDTO(int policyId, String description, UUID eventId) {
        this.policyId = policyId;
        this.description = description;
        this.eventId = eventId;
        //this.companyId = companyId;
    }

    // Getters and Setters
    public int getPolicyId() { return policyId; }
    public void setPolicyId(int policyId) { this.policyId = policyId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    // public int getCompanyId() { return companyId; }
    // public void setCompanyId(int companyId) { this.companyId = companyId; }
}