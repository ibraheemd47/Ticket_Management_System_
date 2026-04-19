package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Application_Layer.PasswordHasher;

@Component
public class password_hash_Imp implements PasswordHasher {

    @Override
    public String hash(String password) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hash'");
    }

    @Override
    public boolean matches(String plainTextPassword, String hashedPassword) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'matches'");
    }

}
