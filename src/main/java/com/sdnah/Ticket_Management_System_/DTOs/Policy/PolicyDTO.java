package com.sdnah.Ticket_Management_System_.DTOs.Policy;

public abstract class PolicyDTO {
    private int policyId;
    private String description;
    private Integer eventId;
    private int companyId;

    public PolicyDTO() {}

    public PolicyDTO(int policyId, String description, Integer eventId, int companyId) {
        this.policyId = policyId;
        this.description = description;
        this.eventId = eventId;
        this.companyId = companyId;
    }

    // Getters and Setters
    public int getPolicyId() { return policyId; }
    public void setPolicyId(int policyId) { this.policyId = policyId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getEventId() { return eventId; }
    public void setEventId(Integer eventId) { this.eventId = eventId; }

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }
}