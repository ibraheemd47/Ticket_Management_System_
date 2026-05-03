package com.sdnah.Ticket_Management_System_.DTOs;

public class VerifyAccountDTO {
    private String username;
    private String code;

    public VerifyAccountDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}