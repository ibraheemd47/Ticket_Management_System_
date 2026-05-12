package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SYSTEM_ADMIN")
public class System_admin extends Member {

    private String who_assigned;

    protected System_admin() {
        // required by JPA
    }

    public System_admin(Member member, String who_assigned_me) {
        super(member.getMemberId(), member.getUsername(), member.getPasswordHash());
        this.role = UserRole.SYSTEM_ADMIN;
        this.who_assigned = who_assigned_me;
        copyProfileFrom(member);
    }

    public System_admin(Member member) {
        super(member.getMemberId(), member.getUsername(), member.getPasswordHash());
        this.who_assigned = "System";
        this.role = UserRole.SYSTEM_ADMIN;
        copyProfileFrom(member);
    }

    private void copyProfileFrom(Member member) {
        this.setActive(member.isActive());
        this.setLoggedin(member.isLoggedin());
        this.setVerified(member.isVerified());
        this.setEmail(member.getEmail());
        this.setPhone(member.getPhone());
        this.setFirstName(member.getFirstName());
        this.setLastName(member.getLastName());
        this.setAddress(member.getAddress());
        this.setCity(member.getCity());
        this.setCountry(member.getCountry());
        this.setBirthDate(member.getBirthDate());
        for (CompanyRoleAssignment role : member.getCompanyRoles()) {
            this.addCompanyRole(role);
        }
    }

    public String getWhoAssigned() {
        return who_assigned;
    }

}
