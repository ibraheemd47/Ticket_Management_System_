package com.sdnah.Ticket_Management_System_.NotificationTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications.NotificationPrincipal;
import com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications.NotificationSessionRegistry;

/**
 * Pure unit tests for the WebSocket session registry + principal value object.
 * No Spring context, no STOMP — just the data structures that track which
 * sessions belong to which member.
 */
class NotificationSessionRegistryTest {

    private NotificationSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new NotificationSessionRegistry();
    }

    // ── register / isConnected ─────────────────────────────────────────────

    @Test
    @DisplayName("register marks the user as connected; isConnected returns true")
    void register_MarksConnected() {
        registry.register("alice", "sess-1");

        assertThat(registry.isConnected("alice")).isTrue();
        assertThat(registry.getSessionCount("alice")).isEqualTo(1);
    }

    @Test
    @DisplayName("Same user with multiple sessions accumulates correctly")
    void register_MultipleSessionsPerUser() {
        registry.register("alice", "sess-1");
        registry.register("alice", "sess-2");
        registry.register("alice", "sess-3");

        assertThat(registry.getSessionCount("alice")).isEqualTo(3);
        assertThat(registry.isConnected("alice")).isTrue();
    }

    @Test
    @DisplayName("Trims whitespace on the member id and session id")
    void register_TrimsWhitespace() {
        registry.register("  alice  ", "  sess  ");
        assertThat(registry.isConnected("alice")).isTrue();
        assertThat(registry.getSessionCount(" alice ")).isEqualTo(1);
    }

    // ── unregister ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("unregisterSession removes that session; remaining sessions keep the user connected")
    void unregisterSession_KeepsUserConnectedIfOthersRemain() {
        registry.register("alice", "sess-1");
        registry.register("alice", "sess-2");

        registry.unregisterSession("sess-1");

        assertThat(registry.isConnected("alice")).isTrue();
        assertThat(registry.getSessionCount("alice")).isEqualTo(1);
    }

    @Test
    @DisplayName("unregisterSession removing the last session drops the user from the registry")
    void unregisterSession_LastOne_RemovesUser() {
        registry.register("alice", "sess-1");

        registry.unregisterSession("sess-1");

        assertThat(registry.isConnected("alice")).isFalse();
        assertThat(registry.getSessionCount("alice")).isZero();
        assertThat(registry.getConnectedUsers()).doesNotContain("alice");
    }

    @Test
    @DisplayName("unregisterSession on an unknown / null / blank id is a silent no-op")
    void unregisterSession_UnknownIsNoop() {
        registry.register("alice", "sess-1");

        registry.unregisterSession("never-existed");
        registry.unregisterSession(null);
        registry.unregisterSession("   ");

        assertThat(registry.isConnected("alice")).isTrue();
    }

    // ── Invalid input ──────────────────────────────────────────────────────

    @Test
    @DisplayName("register rejects null/blank memberId or sessionId")
    void register_InvalidInput_Throws() {
        assertThatThrownBy(() -> registry.register(null, "s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.register("", "s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.register("alice", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.register("alice", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("isConnected and getSessionCount handle null/blank gracefully")
    void readers_NullSafe() {
        assertThat(registry.isConnected(null)).isFalse();
        assertThat(registry.isConnected("")).isFalse();
        assertThat(registry.getSessionCount(null)).isZero();
        assertThat(registry.getSessionCount(" ")).isZero();
    }

    @Test
    @DisplayName("getConnectedUsers returns a snapshot of currently-connected member ids")
    void getConnectedUsers_ReturnsSnapshot() {
        registry.register("alice", "s1");
        registry.register("bob",   "s2");

        assertThat(registry.getConnectedUsers()).containsExactlyInAnyOrder("alice", "bob");
    }

    // ── NotificationPrincipal ──────────────────────────────────────────────

    @Test
    @DisplayName("NotificationPrincipal trims the name and exposes it via getName()")
    void principal_Identity() {
        NotificationPrincipal p = new NotificationPrincipal("  alice  ");
        assertThat(p.getName()).isEqualTo("alice");
    }

    @Test
    @DisplayName("NotificationPrincipal rejects null / blank names")
    void principal_RejectsBlank() {
        assertThatThrownBy(() -> new NotificationPrincipal(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationPrincipal("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
