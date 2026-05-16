package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications;

import java.security.Principal;

public class StompUserPrincipal implements Principal {

    private final String name;

    public StompUserPrincipal(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Principal name cannot be null or blank");
        }
        this.name = name.trim();
    }

    @Override
    public String getName() {
        return name;
    }
}