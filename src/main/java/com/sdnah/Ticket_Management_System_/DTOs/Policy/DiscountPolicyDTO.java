package com.sdnah.Ticket_Management_System_.DTOs.Policy;

import java.util.List;

public class DiscountPolicyDTO extends PolicyDTO {
    private List<DiscountRuleDTO> activeDiscounts;
    private boolean isAdditive;

    public DiscountPolicyDTO() {}

    public DiscountPolicyDTO(int policyId, String description, Integer eventId, int companyId, 
                             List<DiscountRuleDTO> activeDiscounts, boolean isAdditive) {
        super(policyId, description, eventId, companyId);
        this.activeDiscounts = activeDiscounts;
        this.isAdditive = isAdditive;
    }

    // Getters and Setters
    public List<DiscountRuleDTO> getActiveDiscounts() { return activeDiscounts; }
    public void setActiveDiscounts(List<DiscountRuleDTO> activeDiscounts) { this.activeDiscounts = activeDiscounts; }

    public boolean isAdditive() { return isAdditive; }
    public void setAdditive(boolean additive) { isAdditive = additive; }

   
}