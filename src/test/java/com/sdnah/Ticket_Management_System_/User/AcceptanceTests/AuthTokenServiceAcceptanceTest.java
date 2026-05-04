package com.sdnah.Ticket_Management_System_.User.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Application_Layer.AuthTokenService;

class AuthTokenServiceAcceptanceTest {

    private static final String SECRET = "12345678901234567890123456789012"; // 32+ chars for HS256
    private static final long EXPIRATION_MS = 60 * 60 * 1000; // 1 hour

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Given valid username, when generating token, then token is returned")
    void givenValidUsername_WhenGeneratingToken_ThenTokenIsReturned() {
        String token = authTokenService.generateToken("alice");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Given generated token, when validating token, then token is valid")
    void givenGeneratedToken_WhenValidatingToken_ThenTokenIsValid() {
        String token = authTokenService.generateToken("alice");

        assertTrue(authTokenService.validateToken(token));
    }

    @Test
    @DisplayName("Given generated token, when extracting username, then username is returned")
    void givenGeneratedToken_WhenExtractingUsername_ThenUsernameIsReturned() {
        String token = authTokenService.generateToken("alice");

        String username = authTokenService.extractUsername(token);

        assertEquals("alice", username);
    }

    @Test
    @DisplayName("Given generated token, when extracting expiration, then expiration is in the future")
    void givenGeneratedToken_WhenExtractingExpiration_ThenExpirationIsInFuture() {
        String token = authTokenService.generateToken("alice");

        Date expiration = authTokenService.extractExpiration(token);

        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("Given null username, when generating token, then username required exception is thrown")
    void givenNullUsername_WhenGeneratingToken_ThenUsernameRequiredExceptionIsThrown() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authTokenService.generateToken(null));

        assertEquals("Username is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given blank username, when generating token, then username required exception is thrown")
    void givenBlankUsername_WhenGeneratingToken_ThenUsernameRequiredExceptionIsThrown() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authTokenService.generateToken("   "));

        assertEquals("Username is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given invalid token, when validating token, then false is returned")
    void givenInvalidToken_WhenValidatingToken_ThenFalseIsReturned() {
        assertFalse(authTokenService.validateToken("not-a-real-jwt"));
    }

    @Test
    @DisplayName("Given null token, when validating token, then false is returned")
    void givenNullToken_WhenValidatingToken_ThenFalseIsReturned() {
        assertFalse(authTokenService.validateToken(null));
    }

    @Test
    @DisplayName("Given blank token, when extracting username, then token required exception is thrown")
    void givenBlankToken_WhenExtractingUsername_ThenTokenRequiredExceptionIsThrown() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authTokenService.extractUsername("   "));

        assertEquals("Token is required", ex.getMessage());
    }
}