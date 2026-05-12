package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Embeddable
public class CompanyManagerAssignment {

    @Column(name = "manager_id", nullable = false)
    private String managerId;

    @Column(name = "appointed_by_owner_id", nullable = false)
    private String appointedByOwnerId;

    @Convert(converter = CompanyPermissionSetConverter.class)
    @Column(name = "permissions", length = 1024)
    private Set<CompanyPermission> permissions = new HashSet<>();

    protected CompanyManagerAssignment() {
        // required by JPA
    }

    public CompanyManagerAssignment(String managerId,
                                    String appointedByOwnerId,
                                    Set<CompanyPermission> permissions) {
        this.managerId = Objects.requireNonNull(managerId).trim();
        this.appointedByOwnerId = Objects.requireNonNull(appointedByOwnerId).trim();
        this.permissions = permissions == null ? new HashSet<>() : new HashSet<>(permissions);
    }

    public String getManagerId() {
        return managerId;
    }

    public String getAppointedByOwnerId() {
        return appointedByOwnerId;
    }

    public Set<CompanyPermission> getPermissions() {
        return new HashSet<>(permissions);
    }

    public boolean hasPermission(CompanyPermission permission) {
        return permissions.contains(permission);
    }

    public void setPermissions(Set<CompanyPermission> permissions) {
        this.permissions = permissions == null ? new HashSet<>() : new HashSet<>(permissions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompanyManagerAssignment)) return false;
        CompanyManagerAssignment that = (CompanyManagerAssignment) o;
        return Objects.equals(managerId, that.managerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(managerId);
    }
}