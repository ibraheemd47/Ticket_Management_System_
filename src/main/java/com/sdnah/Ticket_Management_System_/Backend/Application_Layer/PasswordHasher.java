package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

public interface PasswordHasher {

    String hash(String plainTextPassword);
    boolean matches(String plainTextPassword, String hashedPassword);
}

