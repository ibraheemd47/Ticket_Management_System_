package com.sdnah.Ticket_Management_System_.Backend.DTOs.Company;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;

// DTO for II.4.15 - View Roles and Permissions.

public class CompanyRolesViewDTO {

    private UUID companyId;
    private final String founderId;
    private final List<String> ownerIds;
    private final Map<String, Set<CompanyPermission>> managerPermissions;

    public CompanyRolesViewDTO(UUID companyId,
                               String founderId,
                               List<String> ownerIds,
                               Map<String, Set<CompanyPermission>> managerPermissions) {
        this.companyId = companyId;
        this.founderId = founderId;
        this.ownerIds = ownerIds;
        this.managerPermissions = managerPermissions;
    }

    public UUID getCompanyId() {
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