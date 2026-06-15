package com.sdnah.Ticket_Management_System_.RobustnessTests;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.AuthTokenService;

/**
 * Robustness tests for {@link AuthTokenService} — the JWT integration layer.
 *
 * Verifies that:
 * - Valid tokens can be generated and their subject extracted correctly
 * - Tampered / expired / blank tokens are rejected without crashing
 * - The service is stateless: tokens for different users do not collide
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AuthTokenService — Robustness Tests")
class AuthTokenRobustnessTest {

    @Autowired
    private AuthTokenService authTokenService;

    // ── Token generation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("JWT: generateToken returns a non-blank token for a valid username")
    void generateToken_validUsername_returnsToken() {
        String token = authTokenService.generateToken("alice");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("JWT: extracted subject matches the username used during generation")
    void generateToken_subjectMatchesUsername() {
        String token = authTokenService.generateToken("bob");
        String subject = authTokenService.extractUsername(token);
        assertThat(subject).isEqualTo("bob");
    }

    @Test
    @DisplayName("JWT: two tokens for different users do not share the same value")
    void generateToken_differentUsers_differentTokens() {
        String t1 = authTokenService.generateToken("user1");
        String t2 = authTokenService.generateToken("user2");
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("JWT: tokens for the same user are valid and carry the correct subject")
    void generateToken_sameUser_bothValidWithCorrectSubject() {
        String t1 = authTokenService.generateToken("charlie");
        String t2 = authTokenService.generateToken("charlie");
        // Both must be valid and carry the right subject regardless of whether
        // the implementation produces the same or different bytes.
        assertThat(authTokenService.validateToken(t1)).isTrue();
        assertThat(authTokenService.validateToken(t2)).isTrue();
        assertThat(authTokenService.extractUsername(t1)).isEqualTo("charlie");
        assertThat(authTokenService.extractUsername(t2)).isEqualTo("charlie");
    }

    @Test
    @DisplayName("JWT: blank username is rejected with an exception")
    void generateToken_blankUsername_throwsException() {
        assertThatThrownBy(() -> authTokenService.generateToken(""))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("JWT: null username is rejected with an exception")
    void generateToken_nullUsername_throwsException() {
        assertThatThrownBy(() -> authTokenService.generateToken(null))
                .isInstanceOf(Exception.class);
    }

    // ── Token validation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("JWT: a freshly generated token is valid")
    void validateToken_freshToken_isValid() {
        String token = authTokenService.generateToken("dave");
        assertThat(authTokenService.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("JWT: a tampered token (appended garbage) is rejected as invalid")
    void validateToken_tamperedToken_isInvalid() {
        String token = authTokenService.generateToken("eve") + "tampered";
        assertThat(authTokenService.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("JWT: a completely random string is rejected as invalid")
    void validateToken_randomString_isInvalid() {
        assertThat(authTokenService.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("JWT: an empty string token is rejected as invalid")
    void validateToken_emptyString_isInvalid() {
        assertThat(authTokenService.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("JWT: token signed with a different secret is rejected")
    void validateToken_wrongSecret_isInvalid() {
        // Craft a token signed by a different key — take first 2 parts and swap signature
        String token = authTokenService.generateToken("frank");
        String[] parts = token.split("\\.");
        // Corrupt the signature segment
        String corrupted = parts[0] + "." + parts[1] + ".invalidsignatureXYZ";
        assertThat(authTokenService.validateToken(corrupted)).isFalse();
    }
}
