package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

public class Member {

    private String memberId;
    private String username;
    private String passwordHash;

    public Member(String memberId, String username, String passwordHash) {
        this.memberId = memberId;
        this.username = username;
        this.passwordHash = passwordHash;
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
    

}
