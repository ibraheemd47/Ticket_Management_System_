package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

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
    }

    public System_admin(Member member) {
        super(member.getMemberId(), member.getUsername(), member.getPasswordHash());
        this.who_assigned = "System";
        this.role = UserRole.SYSTEM_ADMIN;
    }

    public String getWhoAssigned() {
        return who_assigned;
    }

}
