package com.sdnah.Ticket_Management_System_.Domain_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
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
        assertVerifiedMember(actor);
    }

    public void assertCanAssignOwner(Member actor, Company company) {
        assertActiveMember(actor);
        requireCompany(company);

        if (actor.getRoleInCompany(company.getCompanyId()).equals(CompanyRoleType.OWNER)) {
            throw new RuntimeException("Only company owner can assign ownership");
        }
        if (!company.isOwner(actor.getMemberId())) {
            throw new RuntimeException("Only company owner can assign ownership");
        }
    }

    public void assertCanAssignManager(Member actor, Company company) {
        assertActiveMember(actor);
        requireCompany(company);

        if (!company.isOwner(actor.getMemberId())) {
            throw new RuntimeException("Only company owner can assign manager");
        }
    }

    public void assertCanRemoveOwner(Member actor, Company company, String targetOwnerId) {
        assertActiveMember(actor);
        requireCompany(company);

       // int actorId = memberIdAsInt(actor);

        if (!company.isOwner(actor.getMemberId())) {
            throw new RuntimeException("Only company owner can remove ownership");
        }

        if (!company.isOwner(targetOwnerId)) {
            throw new RuntimeException("Target is not an owner in this company");
        }

        if (company.isFounder(targetOwnerId)) {
            throw new RuntimeException("Founder cannot be removed from ownership");
        }
    }

    public void assertCanRemoveManager(Member actor, Company company, String managerId) {
        assertActiveMember(actor);
        requireCompany(company);

       // int actorId = memberIdAsInt(actor);

        if (!company.isOwner(actor.getMemberId())) {
            throw new RuntimeException("Only company owner can remove manager appointment");
        }

        if (!company.isManager(managerId)) {
            throw new RuntimeException("Target is not a manager in this company");
        }
    }

    public void assertCanCloseCompany(Member actor, Company company) {
        assertActiveMember(actor);
        requireCompany(company);

        if (!company.isFounder(actor.getMemberId()) && !(actor.isSystemAdmin())) {
            throw new RuntimeException("Only the founder can close the company");
        }
    }

    public void assertCanReopenCompany(Member actor, Company company) {
        assertActiveMember(actor);
        requireCompany(company);

        if (!company.isFounder(actor.getMemberId())) {
            throw new RuntimeException("Only the founder can reopen the company");
        }
    }

    public void assertCanViewRoles(Member actor, Company company) {
        assertActiveMember(actor);
        requireCompany(company);

        if (!company.isOwner(actor.getMemberId())) {
            throw new RuntimeException("Only a company owner can view roles and permissions");
        }
    }

    public void assertCanModifyManagerPermissions(Member actor, Company company, String managerId) {
        assertActiveMember(actor);
        requireCompany(company);

        //int actorId = memberIdAsInt(actor);

        if (!company.isOwner(actor.getMemberId())) {
            throw new RuntimeException("Only company owner can modify manager permissions");
        }

        if (!company.isManager(managerId)) {
            throw new RuntimeException("Target is not a manager in this company");
        }
    }

    private int memberIdAsInt(Member actor) {
        try {
            return Integer.parseInt(actor.getMemberId());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Member id must be numeric for company operations");
        }
    }
}
