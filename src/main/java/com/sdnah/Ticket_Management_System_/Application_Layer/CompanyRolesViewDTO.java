package com.sdnah.Ticket_Management_System_.Application_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.CompanyPermission;

import java.util.List;
import java.util.Map;
import java.util.Set;

// DTO for II.4.15 - View Roles and Permissions.

public class CompanyRolesViewDTO {

    private final int companyId;
    private final String founderId;
    private final List<String> ownerIds;
    private final Map<String, Set<CompanyPermission>> managerPermissions;

    public CompanyRolesViewDTO(int companyId,
                               String founderId,
                               List<String> ownerIds,
                               Map<String, Set<CompanyPermission>> managerPermissions) {
        this.companyId = companyId;
        this.founderId = founderId;
        this.ownerIds = ownerIds;
        this.managerPermissions = managerPermissions;
    }

    public int getCompanyId() {
        return companyId;
    }

    public String getFounderId() {
        return founderId;
    }

    public List<String> getOwnerIds() {
        return ownerIds;
    }

    public Map<String, Set<CompanyPermission>> getManagerPermissions() {
        return managerPermissions;
    }
}