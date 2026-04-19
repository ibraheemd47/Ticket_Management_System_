package com.sdnah.Ticket_Management_System_.User;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;

class AuthTokenServiceAcceptanceTest {

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService();
    }

    @Test
    void issueToken_withValidMemberId_returnsTokenForThatMember() {
        AuthToken token = authTokenService.issueToken("member-1");

        assertNotNull(token);
        assertNotNull(token.getTokenValue());
        assertEquals("member-1", token.getMemberId());
        assertTrue(token.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void issueToken_withCustomPositiveTtl_returnsTokenWithFutureExpiration() {
        Duration ttl = Duration.ofMinutes(30);

        AuthToken token = authTokenService.issueToken("member-1", ttl);

        assertNotNull(token);
        assertEquals("member-1", token.getMemberId());
        assertTrue(token.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void issueToken_withNullMemberId_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authTokenService.issueToken(null));

        assertEquals("Member ID is required", ex.getMessage());
    }

    @Test
    void issueToken_withBlankMemberId_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authTokenService.issueToken("   "));

        assertEquals("Member ID is required", ex.getMessage());
    }

    @Test
    void issueToken_withZeroTtl_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authTokenService.issueToken("member-1", Duration.ZERO));

        assertEquals("TTL must be a positive duration", ex.getMessage());
    }

    @Test
    void issueToken_withNegativeTtl_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authTokenService.issueToken("member-1", Duration.ofMinutes(-1)));

        assertEquals("TTL must be a positive duration", ex.getMessage());
    }

    @Test
    void isTokenValid_returnsTrueForFreshToken() {
        AuthToken token = authTokenService.issueToken("member-1");

        assertTrue(authTokenService.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        AuthToken expired = new AuthToken("t1", "member-1", LocalDateTime.now().minusMinutes(1));

        assertFalse(authTokenService.isTokenValid(expired));
    }

    @Test
    void isTokenValid_returnsFalseForNullToken() {
        assertFalse(authTokenService.isTokenValid(null));
    }
}
