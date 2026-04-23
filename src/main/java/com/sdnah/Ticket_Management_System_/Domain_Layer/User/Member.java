package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.apache.catalina.User;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
public class Member {

    private String memberId;
    private String username;
    private String passwordHash;
    private boolean active;
    private boolean loggedin;
    protected UserRole role;
    private final Set<CompanyRoleAssignment> companyRoles;

    public Member(String memberId, String username, String passwordHash) {
        if (memberId == null || memberId.isEmpty()) {
            throw new NullPointerException("memberId cannot be null or empty");
        }
        if (username == null || username.isEmpty()) {
            throw new NullPointerException("username cannot be null or empty");
        }
        if (passwordHash == null || passwordHash.isEmpty()) {
            throw new NullPointerException("passwordHash cannot be null or empty");
        }
        this.companyRoles = new java.util.HashSet<>();
        this.memberId = memberId;
        this.username = username;
        this.passwordHash = passwordHash;
        active = true;
        loggedin = true;

        this.role = UserRole.MEMBER;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLoggedin() {
        return loggedin;
    }

    public void login() {
        loggedin = true;
    }

    public void logout() {
        loggedin = false;
    }

    public UserRole getRole() {
        return role;
    }

    public void addCompanyRole(CompanyRoleAssignment assignment) {
        companyRoles.add(assignment);
    }

    public Set<CompanyRoleAssignment> getCompanyRoles() {
        return Collections.unmodifiableSet(companyRoles);
    }

    public Optional<CompanyRoleAssignment> getRoleInCompany(String companyId) {
        return companyRoles.stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .findFirst();
    }

    public boolean isOwnerInCompany(String companyId) {
        return getRoleInCompany(companyId)
                .map(CompanyRoleAssignment::isOwner)
                .orElse(false);
    }

    public boolean isManagerInCompany(String companyId) {
        return getRoleInCompany(companyId)
                .map(CompanyRoleAssignment::isManager)
                .orElse(false);
    }
}
