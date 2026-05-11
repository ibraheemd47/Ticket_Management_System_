package com.sdnah.Ticket_Management_System_.User.UnitTests;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.AuthToken;

@DisplayName("AuthToken — Domain Unit Tests")
class AuthTokenTest {

    private static final String TOKEN_VALUE = "token-abc-123";
    private static final String MEMBER_ID = "member-001";

    @AfterEach
    void teardown() {
    }

    @Test
    @DisplayName("Given valid arguments, when constructed, then tokenValue is stored correctly")
    void givenValidArguments_WhenConstructed_ThenTokenValueIsStoredCorrectly() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        // Act
        AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, expiresAt);

        // Assert
        assertEquals(TOKEN_VALUE, token.getTokenValue());
    }

    @Test
    @DisplayName("Given valid arguments, when constructed, then memberId is stored correctly")
    void givenValidArguments_WhenConstructed_ThenMemberIdIsStoredCorrectly() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        // Act
        AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, expiresAt);

        // Assert
        assertEquals(MEMBER_ID, token.getMemberId());
    }

    @Test
    @DisplayName("Given valid arguments, when constructed, then expiresAt is stored correctly")
    void givenValidArguments_WhenConstructed_ThenExpiresAtIsStoredCorrectly() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        // Act
        AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, expiresAt);

        // Assert
        assertEquals(expiresAt, token.getExpiresAt());
    }

    @Test
    @DisplayName("Given null tokenValue, when constructed, then NullPointerException is thrown")
    void givenNullTokenValue_WhenConstructed_ThenNullPointerExceptionIsThrown() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> new AuthToken(null, MEMBER_ID, expiresAt));
    }

    @Test
    @DisplayName("Given null memberId, when constructed, then NullPointerException is thrown")
    void givenNullMemberId_WhenConstructed_ThenNullPointerExceptionIsThrown() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> new AuthToken(TOKEN_VALUE, null, expiresAt));
    }

    @Test
    @DisplayName("Given null expiresAt, when constructed, then NullPointerException is thrown")
    void givenNullExpiresAt_WhenConstructed_ThenNullPointerExceptionIsThrown() {
        // Arrange & Act & Assert
        assertThrows(NullPointerException.class,
                () -> new AuthToken(TOKEN_VALUE, MEMBER_ID, null));
    }

    // =========================================================================
    // 2. isExpired Tests
    // =========================================================================

    @Test
    @DisplayName("Given expiry in the future, when isExpired is called with now, then returns false")
    void givenFutureExpiry_WhenIsExpiredCalledWithNow_ThenReturnsFalse() {
        // Arrange
        LocalDateTime now = LocalDateTime.of(2025, 6, 1, 12, 0, 0);
        LocalDateTime expiresAt = now.plusMinutes(30);
        AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, expiresAt);

        // Act
        boolean expired = token.isExpired(now);

        // Assert
        assertFalse(expired);
    }

    @Test
    @DisplayName("Given expiry exactly equal to now, when isExpired is called, then returns true")
    void givenExpiryEqualsNow_WhenIsExpiredCalled_ThenReturnsTrue() {
        // Arrange
        LocalDateTime now = LocalDateTime.of(2025, 6, 1, 12, 0, 0);
        AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, now); // expiresAt == now

        // Act
        boolean expired = token.isExpired(now);

        // Assert
        assertTrue(expired);
    }

    @Test
    @DisplayName("Given expiry in the past, when isExpired is called with now, then returns true")
    void givenPastExpiry_WhenIsExpiredCalledWithNow_ThenReturnsTrue() {
        // Arrange
        LocalDateTime now = LocalDateTime.of(2025, 6, 1, 12, 0, 0);
        LocalDateTime expiresAt = now.minusMinutes(1);
        AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, expiresAt);

        // Act
        boolean expired = token.isExpired(now);

        // Assert
        assertTrue(expired);
    }

    @Test
    @DisplayName("Given expiry in the future, when isExpired is called one second before expiry, then returns false")
    void givenFutureExpiry_WhenIsExpiredCalledOneSecondBefore_ThenReturnsFalse() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.of(2025, 6, 1, 12, 0, 0);
        LocalDateTime oneSecBefore = expiresAt.minusSeconds(1);
        AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, expiresAt);

        // Act
        boolean expired = token.isExpired(oneSecBefore);

        // Assert
        assertFalse(expired);
    }

    @Test
    @DisplayName("Given a token, when isExpired is called one second after expiry, then returns true")
    void givenToken_WhenIsExpiredCalledOneSecondAfterExpiry_ThenReturnsTrue() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.of(2025, 6, 1, 12, 0, 0);
        LocalDateTime oneSecAfter = expiresAt.plusSeconds(1);
        AuthToken token = new AuthToken(TOKEN_VALUE, MEMBER_ID, expiresAt);

        // Act
        boolean expired = token.isExpired(oneSecAfter);

        // Assert
        assertTrue(expired);
    }
}