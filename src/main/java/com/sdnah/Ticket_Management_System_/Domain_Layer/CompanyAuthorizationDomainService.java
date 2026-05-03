package com.sdnah.Ticket_Management_System_.Domain_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;

public class CompanyAuthorizationDomainService {
    public Boolean assertActiveMember(Member member) {
        if (member == null || !member.isActive()) {
            throw new RuntimeException("Member must be active to perform this action");
        }
        return true;
    }

    public void assertVerifiedMember(Member actor) {
        assertActiveMember(actor);

        if (!actor.isVerified()) {
            throw new RuntimeException("Only verified members can perform this action");
        }
    }
    protected void requireCompany(Company company) {
        if (company == null) {
            throw new RuntimeException("Company is required");
        }
    }

    public void assertCanOpenCompany(Member actor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'assertCanOpenCompany'");
    }

    
}
