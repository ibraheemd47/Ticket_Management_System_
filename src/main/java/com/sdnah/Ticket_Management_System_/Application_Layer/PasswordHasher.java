package com.sdnah.Ticket_Management_System_.Application_Layer;

public interface PasswordHasher {

    String hash(String plainTextPassword);
    boolean matches(String plainTextPassword, String hashedPassword);
}

