package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PasswordHasher;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Component
public class password_hash_Imp implements PasswordHasher {

    private static final Logger logger = LoggerFactory.getLogger(password_hash_Imp.class);
    private final BCryptPasswordEncoder encoder;

    public password_hash_Imp(@Value("${security.password.bcrypt.strength:12}") int bcryptStrength) {
        this.encoder = new BCryptPasswordEncoder(bcryptStrength);
        logger.info("Password hasher initialized with BCrypt strength={}", bcryptStrength);
    }

    @Override
    public String hash(String password) {
        logger.debug("Password hash request received");
        if (password == null) {
            logger.warn("Password hash rejected: password is null");
            throw new IllegalArgumentException("Password cannot be null");
        }
        String hashed = encoder.encode(password);
        logger.debug("Password hash generated successfully");
        return hashed;
    }

    @Override
    public boolean matches(String plainTextPassword, String hashedPassword) {
        logger.debug("Password match request received");
        if (plainTextPassword == null || hashedPassword == null) {
            logger.warn("Password match rejected: null input detected");
            return false;
        }
        boolean matches = encoder.matches(plainTextPassword, hashedPassword);
        logger.debug("Password match completed with result={}", matches);
        return matches;
    }
}