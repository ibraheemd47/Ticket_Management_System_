package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;
public abstract class Policy {

    private final int policyId;
    private final String description;

    protected Policy(int policyId, String description) {
        this.policyId = policyId;
        this.description = description;
    }

    public int getPolicyId() {
        return policyId;
    }

    public String getDescription() {
        return description;
    }

    public abstract boolean isValid();
}