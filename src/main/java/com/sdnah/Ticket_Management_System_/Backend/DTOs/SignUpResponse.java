package com.sdnah.Ticket_Management_System_.Backend.DTOs;

public class SignUpResponse {
    private boolean success;
    private String message;
    private String memberId;

    public SignUpResponse() {
    }

    public SignUpResponse(boolean success, String message, String memberId) {
        this.success = success;
        this.message = message;
        this.memberId = memberId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
}