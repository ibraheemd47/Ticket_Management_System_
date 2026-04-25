package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;
public abstract class Policy {

    private final int policyId;
    private final String description;
    private final int eventId; // New field to hold the Event ID

    protected Policy(int policyId, String description, int eventId) {
        this.policyId = policyId;
        this.description = description;
        this.eventId = eventId;
    }

    public int getPolicyId() {
        return policyId;
    }

    public String getDescription() {
        return description;
    }

    public int getEventId() {
        return eventId;
    }

    public abstract boolean isValid();
}