package com.sdnah.Ticket_Management_System_.DTOs.Policy;



public class PolicyDTO {

    private int policyId;
    private String description;
    private int eventId;
    private String type; // "PURCHASE", "DISCOUNT", "SELLING"

    public PolicyDTO() {
    }

    public PolicyDTO(int policyId, String description, int eventId, String type) {
        this.policyId = policyId;
        this.description = description;
        this.eventId = eventId;
        this.type = type;
    }

    public int getPolicyId() {
        return policyId;
    }

    public void setPolicyId(int policyId) {
        this.policyId = policyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}