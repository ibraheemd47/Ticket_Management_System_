package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

public class System_admin extends Member {

    private String who_assigned;

    public System_admin(Member member, String who_assigned_me) {
        super(member.getMemberId(), member.getUsername(), member.getPasswordHash());
        this.role = UserRole.SYSTEM_ADMIN;
        this.who_assigned = who_assigned_me;
    }

    public System_admin(Member member) {
        super(member.getMemberId(), member.getUsername(), member.getPasswordHash());
        this.who_assigned = "System";
    }
    public String getWhoAssigned() {
        return who_assigned;
    }

}
