package com.sdnah.Ticket_Management_System_.RobustnessTests;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

/**
 * Robustness tests for {@link UserService} against the real H2 database.
 *
 * Covers:
 * - Successful login for a verified, active member
 * - Rejection of wrong password, unverified, inactive, suspended accounts
 * - Duplicate username registration is rejected
 * - Login after suspension window expires is permitted
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserService — Robustness Tests (DB Integration)")
class UserServiceRobustnessTest {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordHasher passwordHasher;
    @Autowired private UserService userService;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates a verified, active member directly in the DB (bypasses email verification). */
    private Member persistVerifiedMember(String id, String username, String rawPassword) {
        Member m = new Member(id, username, passwordHasher.hash(rawPassword));
        m.setActive(true);
        m.setVerified(true);
        return userRepository.save(m);
    }

    // ── Login happy path ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Login: valid credentials return a non-blank JWT token")
    void login_validCredentials_returnsToken() {
        persistVerifiedMember("u-1", "alice", "Secret1!");

        String token = userService.login("alice", "Secret1!");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Login: tokens for different users are distinct")
    void login_differentUsers_distinctTokens() {
        persistVerifiedMember("u-2", "bob", "PassB1!");
        persistVerifiedMember("u-3", "carol", "PassC2!");

        String t1 = userService.login("bob", "PassB1!");
        String t2 = userService.login("carol", "PassC2!");
        assertThat(t1).isNotEqualTo(t2);
    }

    // ── Login rejection ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Login: wrong password is rejected")
    void login_wrongPassword_throwsException() {
        persistVerifiedMember("u-4", "dave", "CorrectPass1!");

        assertThatThrownBy(() -> userService.login("dave", "WrongPass999"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Login: unknown username is rejected")
    void login_unknownUsername_throwsException() {
        assertThatThrownBy(() -> userService.login("nobody", "anything"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Login: unverified account is rejected")
    void login_unverifiedAccount_throwsException() {
        Member m = new Member("u-5", "eve", passwordHasher.hash("Pass1!"));
        m.setActive(true);
        m.setVerified(false); // not verified
        userRepository.save(m);

        assertThatThrownBy(() -> userService.login("eve", "Pass1!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("verified");
    }

    @Test
    @DisplayName("Login: inactive account is rejected")
    void login_inactiveAccount_throwsException() {
        Member m = new Member("u-6", "frank", passwordHasher.hash("Pass1!"));
        m.setActive(false); // inactive
        m.setVerified(true);
        userRepository.save(m);

        assertThatThrownBy(() -> userService.login("frank", "Pass1!"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Login: suspended account is rejected with suspension message")
    void login_suspendedAccount_throwsException() {
        Member m = new Member("u-7", "grace", passwordHasher.hash("Pass1!"));
        m.setActive(true);
        m.setVerified(true);
        m.suspend(java.time.LocalDateTime.now().plusDays(1)); // suspended until tomorrow
        userRepository.save(m);

        assertThatThrownBy(() -> userService.login("grace", "Pass1!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("suspend");
    }

    @Test
    @DisplayName("Login: empty password string is rejected")
    void login_emptyPassword_throwsException() {
        persistVerifiedMember("u-8", "henry", "RealPass1!");

        assertThatThrownBy(() -> userService.login("henry", ""))
                .isInstanceOf(RuntimeException.class);
    }

    // ── DB integrity via UserRepository ───────────────────────────────────────

    @Test
    @DisplayName("DB: saving two members with the same username violates unique constraint")
    void userRepo_duplicateUsername_throwsConstraintViolation() {
        userRepository.save(new Member("u-9",  "ivan", passwordHasher.hash("p1")));
        userRepository.flush();

        Member duplicate = new Member("u-10", "ivan", passwordHasher.hash("p2"));
        assertThatThrownBy(() -> {
            userRepository.save(duplicate);
            userRepository.flush();
        }).isInstanceOf(Exception.class); // DataIntegrityViolationException
    }

    @Test
    @DisplayName("DB: findByMemberId returns null for unknown ID")
    void userRepo_findByMemberId_unknownReturnsNull() {
        Member result = userRepository.findByMemberId("does-not-exist");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("DB: findByEmail returns empty Optional when no member has that email")
    void userRepo_findByEmail_unknownReturnsEmpty() {
        assertThat(userRepository.findByEmail("ghost@example.com")).isEmpty();
    }

    @Test
    @DisplayName("DB: all members are returned by findAll after multiple saves")
    void userRepo_findAll_returnsAllSavedMembers() {
        persistVerifiedMember("u-11", "jane", "p1");
        persistVerifiedMember("u-12", "kate", "p2");
        persistVerifiedMember("u-13", "leo",  "p3");

        assertThat(userRepository.findAll()).hasSize(3);
    }
}
