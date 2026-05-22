package com.sdnah.Ticket_Management_System_.NotificationTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.INotifier;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.NotificationRepository;

/**
 * Delayed-delivery (queued) tests for the notification flow (issue #96).
 * "Delayed" means: the user was offline when the notification fired, so the
 * row sits in the DB until they sign in again and either:
 *
 *   - fetch their full list via getNotificationsForUser, or
 *   - fetch the unread subset via getUnreadNotificationsForUser, or
 *   - mark something read via markAsRead.
 *
 * These tests exercise that retrieval surface using a mocked repository.
 */
class NotificationDelayedTest {

    private NotificationRepository repo;
    private INotifier notifier;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repo     = mock(NotificationRepository.class);
        notifier = mock(INotifier.class);
        service  = new NotificationService(repo, notifier);
    }

    // ── full list retrieval ────────────────────────────────────────────────

    @Test
    @DisplayName("Offline user has no live push, but the persisted row is what getNotificationsForUser returns")
    void offlineUser_QueuedNotificationVisibleOnFetch() {
        // Pretend the user was offline at write time.
        when(notifier.notifyUser(anyString(), any(NotificationDTO.class))).thenReturn(false);

        // Stub the repo so save+load round-trip works: capture what was saved
        // and return it from findByRecipientUsername.
        java.util.List<Notification> stored = new java.util.ArrayList<>();
        when(repo.save(any(Notification.class))).thenAnswer(inv -> {
            stored.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(repo.findByRecipientUsername("alice")).thenReturn(stored);

        service.createNotification("alice", "queued msg", NotificationType.GENERIC);
        service.createNotification("alice", "another",  NotificationType.GENERIC);

        List<NotificationDTO> fetched = service.getNotificationsForUser("alice");
        assertThat(fetched).hasSize(2);
        assertThat(fetched).extracting(NotificationDTO::getMessage)
                .containsExactlyInAnyOrder("queued msg", "another");
        // Saved twice, push attempted twice but each returned false → no exception.
        verify(repo, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("getNotificationsForUser returns empty list when the user has nothing queued")
    void getNotificationsForUser_EmptyByDefault() {
        when(repo.findByRecipientUsername("alice")).thenReturn(List.of());
        assertThat(service.getNotificationsForUser("alice")).isEmpty();
    }

    @Test
    @DisplayName("getNotificationsForUser trims whitespace from the lookup key")
    void getNotificationsForUser_TrimsLookup() {
        when(repo.findByRecipientUsername("alice")).thenReturn(List.of());
        service.getNotificationsForUser("  alice  ");
        verify(repo).findByRecipientUsername("alice");
    }

    @Test
    @DisplayName("getNotificationsForUser rejects blank username")
    void getNotificationsForUser_BlankRejected() {
        assertThatThrownBy(() -> service.getNotificationsForUser(""))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).findByRecipientUsername(anyString());
    }

    // ── unread subset ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getUnreadNotificationsForUser returns only rows where read=false")
    void getUnreadNotificationsForUser_OnlyUnread() {
        Notification unread = new Notification("alice", "u", NotificationType.GENERIC);
        Notification read   = new Notification("alice", "r", NotificationType.GENERIC);
        read.markAsRead();

        when(repo.findByRecipientUsernameAndReadFalse("alice"))
                .thenReturn(List.of(unread));

        List<NotificationDTO> out = service.getUnreadNotificationsForUser("alice");
        assertThat(out).hasSize(1);
        assertThat(out.get(0).isRead()).isFalse();
        verify(repo).findByRecipientUsernameAndReadFalse("alice");
    }

    // ── markAsRead ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAsRead flips the read flag and saves the notification")
    void markAsRead_HappyPath() {
        Notification n = new Notification("alice", "msg", NotificationType.GENERIC);
        when(repo.findById(n.getId())).thenReturn(Optional.of(n));

        service.markAsRead(n.getId(), "alice");

        assertThat(n.isRead()).isTrue();
        verify(repo).save(n);
    }

    @Test
    @DisplayName("markAsRead refuses to flip someone else's notification")
    void markAsRead_WrongOwner_Throws() {
        Notification n = new Notification("alice", "msg", NotificationType.GENERIC);
        when(repo.findById(n.getId())).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.markAsRead(n.getId(), "bob"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(n.isRead()).isFalse();
        verify(repo, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead on a missing id throws not-found")
    void markAsRead_MissingId_Throws() {
        when(repo.findById("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markAsRead("ghost", "alice"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead rejects blank id and never touches the repo")
    void markAsRead_BlankId_Rejected() {
        assertThatThrownBy(() -> service.markAsRead("", "alice"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).findById(anyString());
    }
}
