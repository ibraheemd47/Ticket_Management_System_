package com.sdnah.Ticket_Management_System_.NotificationTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.INotifier;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.NotificationRepository;

/**
 * Real-time delivery tests for the notification flow (issue #95). The existing
 * NotificationsServiceUnitTest mocks {@link INotifier} but never verifies it
 * was invoked — these tests pin the realtime contract:
 *
 *   - When createNotification runs and the user IS connected, notifier.notifyUser(...)
 *     is called with a DTO whose fields match the persisted notification.
 *   - When notifyUser returns false (user offline), the notification is still
 *     persisted and no exception propagates.
 *   - When notifyUser throws, the same — persistence wins, push errors don't.
 *   - The "convenience" senders (notifyPurchaseSuccess etc.) route through
 *     createNotification with the right NotificationType.
 */
class NotificationRealtimeTest {

    private NotificationRepository repo;
    private INotifier notifier;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repo     = mock(NotificationRepository.class);
        notifier = mock(INotifier.class);
        service  = new NotificationService(repo, notifier);
    }

    @Test
    @DisplayName("createNotification pushes a real-time DTO matching the persisted notification")
    void realtimePush_OccursOnCreate() {
        // notifier returns true → "delivered"
        when(notifier.notifyUser(anyString(), any(NotificationDTO.class))).thenReturn(true);

        service.createNotification("alice", "Welcome", NotificationType.SYSTEM_ANNOUNCEMENT);

        // Notification persisted exactly once.
        ArgumentCaptor<Notification> savedCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(repo, times(1)).save(savedCaptor.capture());

        // Push fired exactly once with a DTO derived from the same notification.
        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(notifier, times(1)).notifyUser(eq("alice"), dtoCaptor.capture());

        NotificationDTO sent = dtoCaptor.getValue();
        assertThat(sent.getMessage()).isEqualTo("Welcome");
        assertThat(sent.getType()).isEqualTo(NotificationType.SYSTEM_ANNOUNCEMENT);
        assertThat(sent.getRecipientUsername()).isEqualTo("alice");
        // The persisted notification still has the same id the DTO reports.
        assertThat(sent.getId()).isEqualTo(savedCaptor.getValue().getId());
    }

    @Test
    @DisplayName("Recipient is trimmed before push so whitespace doesn't break broadcaster routing")
    void realtimePush_TrimsRecipient() {
        when(notifier.notifyUser(anyString(), any(NotificationDTO.class))).thenReturn(true);

        service.createNotification("  alice  ", "x", NotificationType.GENERIC);

        verify(notifier).notifyUser(eq("alice"), any(NotificationDTO.class));
    }

    @Test
    @DisplayName("If the user is offline (notifier returns false), the notification is still saved and no exception")
    void offlineRecipient_PersistsButDoesNotFail() {
        when(notifier.notifyUser(anyString(), any(NotificationDTO.class))).thenReturn(false);

        String id = service.createNotification(
                "alice", "queued", NotificationType.SYSTEM_ANNOUNCEMENT);

        assertThat(id).isNotBlank();
        verify(repo, times(1)).save(any(Notification.class));
        verify(notifier, times(1)).notifyUser(eq("alice"), any(NotificationDTO.class));
    }

    @Test
    @DisplayName("Realtime delivery failure is swallowed — the notification still persists")
    void realtimeDeliveryThrows_StillPersists() {
        when(notifier.notifyUser(anyString(), any(NotificationDTO.class)))
                .thenThrow(new RuntimeException("ws disconnected"));

        // The call must NOT throw; persistence is the source of truth.
        String id = service.createNotification(
                "alice", "msg", NotificationType.GENERIC);

        assertThat(id).isNotBlank();
        verify(repo, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Invalid input throws BEFORE any push attempt")
    void invalidInput_NeverHitsNotifier() {
        when(notifier.notifyUser(anyString(), any(NotificationDTO.class))).thenReturn(true);

        assertThatThrownBy(() -> service.createNotification(
                "alice", "", NotificationType.GENERIC))
                .isInstanceOf(IllegalArgumentException.class);

        // No push attempted on a rejected request.
        verify(notifier, times(0)).notifyUser(anyString(), any());
    }

    // ── convenience senders ────────────────────────────────────────────────

    @Test
    @DisplayName("notifyPurchaseSuccess routes through createNotification with PURCHASE_SUCCESS")
    void notifyPurchaseSuccess_RoutesWithCorrectType() {
        when(notifier.notifyUser(anyString(), any(NotificationDTO.class))).thenReturn(true);

        service.notifyPurchaseSuccess("alice", "Coldplay 26");

        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(notifier).notifyUser(eq("alice"), dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().getType()).isEqualTo(NotificationType.PURCHASE_SUCCESS);
        assertThat(dtoCaptor.getValue().getMessage()).contains("Coldplay 26");
    }

    @Test
    @DisplayName("notifyEventCancelled routes through createNotification with EVENT_CANCELLED")
    void notifyEventCancelled_RoutesWithCorrectType() {
        when(notifier.notifyUser(anyString(), any(NotificationDTO.class))).thenReturn(true);

        service.notifyEventCancelled("alice", "Eras Tour");

        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(notifier).notifyUser(eq("alice"), dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().getType()).isEqualTo(NotificationType.EVENT_CANCELLED);
        assertThat(dtoCaptor.getValue().getMessage()).contains("Eras Tour");
    }
}
