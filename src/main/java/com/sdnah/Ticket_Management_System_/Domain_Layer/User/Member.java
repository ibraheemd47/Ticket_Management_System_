package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
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

    @ElementCollection
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

    protected Member() {
        // Required by JPA
    }

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
        this.active = true;
        this.loggedin = false;
        this.verified = false;
        this.role = UserRole.MEMBER;
        this.companyRoles = new HashSet<>();
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
}