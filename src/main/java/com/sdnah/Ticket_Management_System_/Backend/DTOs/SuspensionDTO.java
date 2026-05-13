package com.sdnah.Ticket_Management_System_.Backend.DTOs;

import java.time.LocalDateTime;

public class SuspensionDTO {
    private String memberId;
    private String username;
    private LocalDateTime suspensionStartedAt;
    private LocalDateTime suspendedUntil;       
    private boolean suspendedPermanently;

    public SuspensionDTO(String memberId, String username,
                         LocalDateTime suspensionStartedAt,
                         LocalDateTime suspendedUntil,
                         boolean suspendedPermanently) {
        this.memberId = memberId;
        this.username = username;
        this.suspensionStartedAt = suspensionStartedAt;
        this.suspendedUntil = suspendedUntil;
        this.suspendedPermanently = suspendedPermanently;
    }

    public String getMemberId() { return memberId; }
    public String getUsername() { return username; }
    public LocalDateTime getSuspensionStartedAt() { return suspensionStartedAt; }
    public LocalDateTime getSuspendedUntil() { return suspendedUntil; }
    public boolean isSuspendedPermanently() { return suspendedPermanently; }
}