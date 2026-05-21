package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.UserDTO;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public class Member {

    @Id
    private String memberId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private boolean active;
    private boolean loggedin;

    protected UserRole role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_company_roles", joinColumns = @JoinColumn(name = "member_id"))
    private Set<CompanyRoleAssignment> companyRoles = new HashSet<>();

    // Information fields for profile / information page
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private LocalDate birthDate;
    // Fields for verification process
    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;
    private boolean verified;

    //for version2 suspension and reactivation
    @Column(name = "suspension_start_date")
    private LocalDateTime suspensionStartedAt;

    @Column(name = "suspension_end_date")
    private LocalDateTime suspendedUntil;

    @Column(name = "suspended_permanently")
    private boolean suspendedPermanently;

    // rest
    private String passwordResetCode;
    private java.time.LocalDateTime passwordResetCodeExpiresAt;

    protected Member() {
        // Required by JPA
    }

    public Member(String memberId, String username, String passwordHash) {

        if (memberId == null || memberId.isEmpty()) {
            throw new IllegalArgumentException("memberId cannot be null or empty");
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
        this.active = true;
        this.loggedin = false;
        this.verified = false;
        this.role = UserRole.MEMBER;
        this.companyRoles = new HashSet<>();
    }

    public UserDTO toUserDTO() {
        UserDTO dto = new UserDTO( );
        dto.setMemberId(this.memberId);
        dto.setUsername(this.username);
        dto.setEmail(this.email);
        dto.setActive(this.active);
        return dto;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isLoggedin() {
        return loggedin;
    }

    public void setLoggedin(boolean loggedin) {
        this.loggedin = loggedin;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isSystemAdmin() {
        return role == UserRole.SYSTEM_ADMIN;
    }
    public void setRole(UserRole role) {
        this.role = role;
    }

    public Set<CompanyRoleAssignment> getCompanyRoles() {
        return Collections.unmodifiableSet(companyRoles);
    }

    public void setCompanyRoles(Set<CompanyRoleAssignment> companyRoles) {
        this.companyRoles = companyRoles == null ? new HashSet<>() : new HashSet<>(companyRoles);
    }

    public void addCompanyRole(CompanyRoleAssignment assignment) {
        companyRoles.add(assignment);
    }

    public Optional<CompanyRoleAssignment> getRoleInCompany(UUID companyId) {
        return companyRoles.stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .findFirst();
    }

    public boolean isOwnerInCompany(UUID companyId) {
        return getRoleInCompany(companyId)
                .map(CompanyRoleAssignment::isOwner)
                .orElse(false);
    }

    public boolean isManagerInCompany(UUID companyId) {
        return getRoleInCompany(companyId)
                .map(CompanyRoleAssignment::isManager)
                .orElse(false);
    }

    public void login() {
        this.loggedin = true;
    }

    public void logout() {
        this.loggedin = false;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getFullName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getVerificationCodeExpiresAt() {
        return verificationCodeExpiresAt;
    }

    public void setVerificationCodeExpiresAt(LocalDateTime expiresAt) {
        this.verificationCodeExpiresAt = expiresAt;
    }

    public String getPasswordResetCode() {
        return passwordResetCode;
    }

    public void setPasswordResetCode(String passwordResetCode) {
        this.passwordResetCode = passwordResetCode;
    }

    public java.time.LocalDateTime getPasswordResetCodeExpiresAt() {
        return passwordResetCodeExpiresAt;
    }

    public void setPasswordResetCodeExpiresAt(java.time.LocalDateTime passwordResetCodeExpiresAt) {
        this.passwordResetCodeExpiresAt = passwordResetCodeExpiresAt;
    }

    public void clearPasswordResetCode() {
        this.passwordResetCode = null;
        this.passwordResetCodeExpiresAt = null;
    }


    ///version2 suspension and reactivation
    public boolean isSuspended() {
        if (suspendedPermanently) return true;
        if (suspendedUntil == null) return false;
        return LocalDateTime.now().isBefore(suspendedUntil);
    }

    public LocalDateTime getSuspendedUntil() { return suspendedUntil; }
    public boolean isSuspendedPermanently() { return suspendedPermanently; }
    public LocalDateTime getSuspensionStartedAt() { return suspensionStartedAt; }


    public void suspend(LocalDateTime until) {
        this.suspensionStartedAt = LocalDateTime.now(); 
        if (until == null) {
            suspendPermanently();
        } else {
            this.suspendedPermanently = false;
            this.suspendedUntil = until;
        }
    }

    public void suspendPermanently() 
    {
        this.suspensionStartedAt = LocalDateTime.now(); // ← הוסף
        this.suspendedPermanently = true;
        this.suspendedUntil = null;
    }

    public void unsuspend() 
    {
        this.suspendedPermanently = false;
        this.suspendedUntil = null;
        this.suspensionStartedAt = null; 
    }
}