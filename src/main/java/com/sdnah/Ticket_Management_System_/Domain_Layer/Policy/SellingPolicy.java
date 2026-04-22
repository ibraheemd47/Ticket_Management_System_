package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;



public class SellingPolicy extends Policy {
    public enum SellingType { 
        REGULAR, 
        LOTTERY 
    }
    private SellingType type;

    public SellingPolicy(int policyId, String description, SellingType type) {
        super(policyId, description);
        this.type = type;
    }

    @Override
    public boolean isValid() { return true; }

    // --- II.2.5: Select Tickets for Event ---
    public boolean isSelectionAllowed(boolean isMember) {
        if (this.type == SellingType.LOTTERY) {
            return isMember; 
        }
        return true; 
    }
}