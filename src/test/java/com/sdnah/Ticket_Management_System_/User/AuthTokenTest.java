package com.sdnah.Ticket_Management_System_.User;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;

class AuthTokenTest {

    @Test
    void constructor_storesFieldsCorrectly() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        AuthToken token = new AuthToken("token-123", "member-1", expiresAt);

        assertEquals("token-123", token.getTokenValue());
        assertEquals("member-1", token.getMemberId());
        assertEquals(expiresAt, token.getExpiresAt());
    }

    @Test
    void isExpired_returnsFalseWhenNowIsBeforeExpiration() {
        LocalDateTime now = LocalDateTime.now();
        AuthToken token = new AuthToken("token-123", "member-1", now.plusMinutes(10));

        assertFalse(token.isExpired(now));
    }

    @Test
    void isExpired_returnsTrueWhenNowEqualsExpiration() {
        LocalDateTime now = LocalDateTime.now();
        AuthToken token = new AuthToken("token-123", "member-1", now);

        assertTrue(token.isExpired(now));
    }

    @Test
    void isExpired_returnsTrueWhenNowIsAfterExpiration() {
        LocalDateTime now = LocalDateTime.now();
        AuthToken token = new AuthToken("token-123", "member-1", now.minusMinutes(1));

        assertTrue(token.isExpired(now));
    }
}
