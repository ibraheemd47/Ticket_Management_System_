package com.sdnah.Ticket_Management_System_.Application_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.CompanyPermission;

import java.util.List;
import java.util.Map;
import java.util.Set;

// DTO for II.4.15 - View Roles and Permissions.

public class CompanyRolesViewDTO {

    private final int companyId;
    private final int founderId;
    private final List<Integer> ownerIds;
    private final Map<Integer, Set<CompanyPermission>> managerPermissions;

    public CompanyRolesViewDTO(int companyId,
                               int founderId,
                               List<Integer> ownerIds,
                               Map<Integer, Set<CompanyPermission>> managerPermissions) {
        this.companyId = companyId;
        this.founderId = founderId;
        this.ownerIds = ownerIds;
        this.managerPermissions = managerPermissions;
    }

    public int getCompanyId() {
        return companyId;
    }

    public int getFounderId() {
        return founderId;
    }

    public List<Integer> getOwnerIds() {
        return ownerIds;
    }

    public Map<Integer, Set<CompanyPermission>> getManagerPermissions() {
        return managerPermissions;
    }
}
