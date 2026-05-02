package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;

// import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy.DiscountRule;

public class PurchasePolicy extends Policy {
    private int minTickets = 1;
    private int maxTickets = Integer.MAX_VALUE;
    private int minAge = 0;
    private boolean allowSingleSeatGap = true;

    public PurchasePolicy(int policyId, String description, int eventId, int companyId) {
        super(policyId, description, eventId, companyId);
    }

    // II.2.4 (Reserve) II.2.8 (Checkout)
    public boolean validatePurchase(int quantity, int userAge, boolean createsSingleGap) {
        if (quantity < minTickets || quantity > maxTickets)     
            return false;
        if (userAge < minAge) 
            return false;
        if (!allowSingleSeatGap && createsSingleGap) 
            return false;
        return true;
    }
    
   

    // --- Specific Constraint Updates (Use Case II.4.3) --- for the next version

    // /**
    //  * Updates the minimum number of tickets required per purchase.
    //  */
    // public void setMinTickets(int minTickets) {
    //     if (minTickets < 0) throw new IllegalArgumentException("Minimum tickets cannot be negative");
    //     this.minTickets = minTickets;
    // }

    // /**
    //  * Updates the maximum number of tickets allowed per purchase.
    //  */
    // public void setMaxTickets(int maxTickets) {
    //     if (maxTickets < minTickets) throw new IllegalArgumentException("Maximum tickets cannot be less than minimum");
    //     this.maxTickets = maxTickets;
    // }

    // /**
    //  * Updates the minimum age requirement for the event.
    //  */
    // public void setMinAge(int minAge) {
    //     if (minAge < 0) throw new IllegalArgumentException("Minimum age cannot be negative");
    //     this.minAge = minAge;
    // }

    // /**
    //  * Updates the policy regarding single seat gaps in the venue.
    //  * @param allow true if leaving a single seat empty is permitted.
    //  */
    // public void setAllowSingleSeatGap(boolean allow) {
    //     this.allowSingleSeatGap = allow;
    // }

    @Override
    public boolean isValid() { return minTickets <= maxTickets && minAge >= 0; }
}
    

