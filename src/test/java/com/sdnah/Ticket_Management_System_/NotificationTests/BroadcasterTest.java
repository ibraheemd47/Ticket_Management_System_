package com.sdnah.Ticket_Management_System_.NotificationTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications.Broadcaster;
import com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications.NotificationSessionRegistry;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;

/**
 * Unit tests for the WebSocket push transport layer:
 *  - Broadcaster delegates registration to {@link NotificationSessionRegistry},
 *    only pushes when the user is connected, and validates its inputs.
 */
class BroadcasterTest {

    private SimpMessagingTemplate template;
    private NotificationSessionRegistry registry;
    private Broadcaster broadcaster;

    @BeforeEach
    void setUp() {
        template = mock(SimpMessagingTemplate.class);
        registry = mock(NotificationSessionRegistry.class);
        broadcaster = new Broadcaster(template, registry);
    }

    private NotificationDTO dto() {
        return new NotificationDTO("id-1", "alice", "hello",
                NotificationType.GENERIC, LocalDateTime.now(), false);
    }

    // ── Pass-through methods ───────────────────────────────────────────────

    @Test
    @DisplayName("register / unregister / isConnected / getSessionCount delegate to the registry")
    void delegatesToRegistry() {
        broadcaster.register("alice", "sess-1");
        verify(registry).register("alice", "sess-1");

        broadcaster.unregister("sess-1");
        verify(registry).unregisterSession("sess-1");

        when(registry.isConnected("alice")).thenReturn(true);
        when(registry.getSessionCount("alice")).thenReturn(2);

        assertThat(broadcaster.isConnected("alice")).isTrue();
        assertThat(broadcaster.getSessionCount("alice")).isEqualTo(2);
    }

    // ── broadcast happy / sad paths ────────────────────────────────────────

    @Test
    @DisplayName("broadcast: when user is connected, pushes to /queue/notifications and returns true")
    void broadcast_ConnectedUser_Pushes() {
        when(registry.isConnected("alice")).thenReturn(true);

        boolean delivered = broadcaster.broadcast("alice", dto());

        assertThat(delivered).isTrue();
        verify(template).convertAndSendToUser(
                eq("alice"),
                eq("/queue/notifications"),
                any(NotificationDTO.class));
    }

    @Test
    @DisplayName("broadcast: trims whitespace from recipient before routing")
    void broadcast_TrimsRecipient() {
        when(registry.isConnected("alice")).thenReturn(true);

        broadcaster.broadcast("  alice  ", dto());

        verify(template).convertAndSendToUser(
                eq("alice"),
                eq("/queue/notifications"),
                any(NotificationDTO.class));
    }

    @Test
    @DisplayName("broadcast: offline user → false, no push to template")
    void broadcast_OfflineUser_ReturnsFalseAndSkipsPush() {
        when(registry.isConnected("alice")).thenReturn(false);

        boolean delivered = broadcaster.broadcast("alice", dto());

        assertThat(delivered).isFalse();
        verify(template, never()).convertAndSendToUser(any(String.class), any(String.class), any());
    }

    // ── Input validation ───────────────────────────────────────────────────

    @Test
    @DisplayName("broadcast rejects null/blank recipient and null notification")
    void broadcast_InvalidInput_Throws() {
        assertThatThrownBy(() -> broadcaster.broadcast(null, dto()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> broadcaster.broadcast("   ", dto()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> broadcaster.broadcast("alice", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(template, never()).convertAndSendToUser(any(String.class), any(String.class), any());
    }
}
