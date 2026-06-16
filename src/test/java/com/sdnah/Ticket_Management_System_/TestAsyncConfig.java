package com.sdnah.Ticket_Management_System_;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * In the "test" profile, replace Spring's async executor with a synchronous
 * (caller-thread) executor so that @Async methods run inline.
 *
 * This is needed because VerificationEmail.createAndSendCode() is @Async and
 * mutates the Member entity (sets verificationCode). If it runs in a background
 * thread the mutation happens on a detached object and is never persisted,
 * causing verifyAccount() to fail with "Verification code is required".
 */
@Configuration
@Profile("test")
public class TestAsyncConfig {

    @Bean
    @Primary
    public Executor taskExecutor() {
        // Run @Async methods synchronously on the calling thread
        return Runnable::run;
    }
}
