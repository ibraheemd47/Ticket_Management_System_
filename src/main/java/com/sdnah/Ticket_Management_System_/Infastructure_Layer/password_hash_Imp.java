package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sdnah.Ticket_Management_System_.Application_Layer.PasswordHasher;
@Component
public class password_hash_Imp implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return encoder.encode(password);
    }

    @Override
    public boolean matches(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        return encoder.matches(plainTextPassword, hashedPassword);
    }
}