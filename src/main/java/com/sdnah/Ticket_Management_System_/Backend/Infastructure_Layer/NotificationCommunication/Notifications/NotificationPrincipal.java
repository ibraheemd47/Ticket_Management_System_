package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.NotificationCommunication.Notifications;

import java.security.Principal;

public class NotificationPrincipal implements Principal {

    private final String member_id;

    public NotificationPrincipal(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Principal name cannot be null or blank");
        }
        this.member_id = memberId.trim();
    }

    @Override
    public String getName() {
        return member_id;
    }
}