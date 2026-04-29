package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class CompanyRoleAssignment {

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private CompanyRoleType roleType;

    @Convert(converter = ManagerPermissionSetConverter.class)
    @Column(name = "permissions", length = 1024)
    private Set<ManagerPermission> permissions = new HashSet<>();

    @Column(name = "appointed_by_member_id")
    private String appointedByMemberId;

    protected CompanyRoleAssignment() {
        this.permissions = new HashSet<>();
    }

    public CompanyRoleAssignment(String companyId, String appointedByMemberId, CompanyRoleType roleType,
            Set<ManagerPermission> permissions) {
        this.companyId = Objects.requireNonNull(companyId);
        this.appointedByMemberId = appointedByMemberId;
        this.roleType = Objects.requireNonNull(roleType);
        this.permissions = permissions == null ? new HashSet<>() : new HashSet<>(permissions);
    }

    public void addPermission(ManagerPermission permission) {
        if (!isManager()) {
            throw new RuntimeException("Only managers can have manager permissions");
        }
        permissions.add(permission);
    }

    public boolean hasPermission(ManagerPermission permission) {
        return permissions.contains(permission);
    }

    public boolean isOwner() {
        return roleType == CompanyRoleType.OWNER;
    }

    public boolean isManager() {
        return roleType == CompanyRoleType.MANAGER;
    }

    public String getCompanyId() {
        return companyId;
    }

    public CompanyRoleType getRoleType() {
        return roleType;
    }

    public void removePermission(ManagerPermission permission) {
        permissions.remove(permission);
    }

    public Set<ManagerPermission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public String getAppointedByMemberId() {
        return appointedByMemberId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CompanyRoleAssignment))
            return false;
        CompanyRoleAssignment that = (CompanyRoleAssignment) o;
        return Objects.equals(companyId, that.companyId)
                && roleType == that.roleType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, roleType);
    }
}
