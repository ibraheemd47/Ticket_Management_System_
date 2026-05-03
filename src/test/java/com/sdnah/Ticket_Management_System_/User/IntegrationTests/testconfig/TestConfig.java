package com.sdnah.Ticket_Management_System_.User.IntegrationTests.testconfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.sdnah.Ticket_Management_System_.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.VerificationEmail;

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
