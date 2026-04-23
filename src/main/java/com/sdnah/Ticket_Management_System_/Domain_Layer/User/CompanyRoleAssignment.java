package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "company_role_assignments")
public class CompanyRoleAssignment {
    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    private String companyId;
    private CompanyRoleType roleType;
    private Set<ManagerPermission> permissions;
    private String appointedByMemberId; // who appointed this user

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

    private static Set<ManagerPermission> convertPermissions(Set<String> permissionStrings) {
        Set<ManagerPermission> permissions = new HashSet<>();
        for (String perm : permissionStrings) {
            permissions.add(ManagerPermission.valueOf(perm));
        }
        return permissions;
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
}
