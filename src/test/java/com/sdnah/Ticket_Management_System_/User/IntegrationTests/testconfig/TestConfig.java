package com.sdnah.Ticket_Management_System_.User.IntegrationTests.testconfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.VerificationEmail;

@TestConfiguration
public class TestConfig {

    @Bean
    public PasswordHasher passwordHasher() {
        return new FakePasswordHasher();
    }

    @Bean
    public VerificationEmail verificationEmail() {
        return new FakeVerificationEmail();
    }
}
