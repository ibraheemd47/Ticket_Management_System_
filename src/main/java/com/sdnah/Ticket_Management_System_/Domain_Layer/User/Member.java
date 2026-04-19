package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

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

    public Member(String memberId, String username, String passwordHash) {
        this.memberId = memberId;
        this.username = username;
        this.passwordHash = passwordHash;
        active = true ;
        loggedin= true ; 

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
        return active;      }
    public boolean isLoggedin() {
        return loggedin;}

    public void login () {
        loggedin = true;
    }
    public void logout () {
        loggedin = false;
    }

    public UserRole getRole() {
        return role;
     }
     

}
