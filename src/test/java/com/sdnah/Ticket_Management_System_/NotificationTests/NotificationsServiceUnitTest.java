package com.sdnah.Ticket_Management_System_.NotificationTests;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.INotifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class NotificationsServiceUnitTest {

    private NotificationService notificationsService;
    private NotificationRepository notificationRepository;
    private INotifier realtimeNotificationSender;

    @BeforeEach
        void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        realtimeNotificationSender = mock(INotifier.class);

        notificationsService = new NotificationService(
                notificationRepository,
                realtimeNotificationSender
        );
        }

    @Test
    void createNotification_validInput_returnsNotificationId() {
        String id = notificationsService.createNotification(
                "hadeel",
                "Company updated",
                NotificationType.SYSTEM_ANNOUNCEMENT
        );

        assertNotNull(id);
        assertFalse(id.isBlank());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_thenGetNotificationsForUser_returnsStoredNotification() {
        Notification notification = new Notification(
                "hadeel",
                "You have a new notification",
                NotificationType.GENERIC
        );

        when(notificationRepository.findByRecipientUsername("hadeel"))
                .thenReturn(List.of(notification));

        List<NotificationDTO> notifications =
                notificationsService.getNotificationsForUser("hadeel");

        assertEquals(1, notifications.size());
        assertEquals("hadeel", notifications.get(0).getRecipientUsername());
        assertEquals("You have a new notification", notifications.get(0).getMessage());
        assertEquals(NotificationType.GENERIC, notifications.get(0).getType());
    }

    @Test
    void getNotificationsForUser_returnsOnlyMatchingUserNotifications() {
        Notification n1 = new Notification("hadeel", "msg1", NotificationType.GENERIC);
        Notification n2 = new Notification("hadeel", "msg3", NotificationType.SYSTEM_ANNOUNCEMENT);

        when(notificationRepository.findByRecipientUsername("hadeel"))
                .thenReturn(List.of(n1, n2));

        List<NotificationDTO> notifications =
                notificationsService.getNotificationsForUser("hadeel");

        assertEquals(2, notifications.size());
        assertTrue(notifications.stream()
                .allMatch(notification -> notification.getRecipientUsername().equals("hadeel")));
    }

    @Test
    void getNotificationsForUser_whenUserHasNoNotifications_returnsEmptyList() {
        when(notificationRepository.findByRecipientUsername("unknownUser"))
                .thenReturn(List.of());

        List<NotificationDTO> notifications =
                notificationsService.getNotificationsForUser("unknownUser");

        assertNotNull(notifications);
        assertTrue(notifications.isEmpty());
    }

    @Test
    void createNotification_withBlankRecipient_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.createNotification("   ", "message", NotificationType.GENERIC));
    }

    @Test
    void createNotification_withBlankMessage_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.createNotification("hadeel", "   ", NotificationType.GENERIC));
    }

    @Test
    void createNotification_withNullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.createNotification("hadeel", "message", null));
    }

    // ── getUnreadNotificationsForUser ─────────────────────────────────────────

    @Test
    void getUnreadNotificationsForUser_returnsOnlyUnread() {
        Notification unread = new Notification("hadeel", "unread msg", NotificationType.GENERIC);

        when(notificationRepository.findByRecipientUsernameAndReadFalse("hadeel"))
                .thenReturn(List.of(unread));

        List<NotificationDTO> result = notificationsService.getUnreadNotificationsForUser("hadeel");

        assertEquals(1, result.size());
        assertEquals("unread msg", result.get(0).getMessage());
    }

    @Test
    void getUnreadNotificationsForUser_noUnread_returnsEmpty() {
        when(notificationRepository.findByRecipientUsernameAndReadFalse("hadeel"))
                .thenReturn(List.of());

        List<NotificationDTO> result = notificationsService.getUnreadNotificationsForUser("hadeel");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUnreadNotificationsForUser_blankUsername_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.getUnreadNotificationsForUser("   "));
    }

    // ── markAsRead ────────────────────────────────────────────────────────────

    @Test
    void markAsRead_validNotification_marksAndSaves() {
        Notification n = new Notification("hadeel", "msg", NotificationType.GENERIC);

        when(notificationRepository.findById("notif-1")).thenReturn(java.util.Optional.of(n));

        notificationsService.markAsRead("notif-1", "hadeel");

        assertTrue(n.isRead());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAsRead_notificationNotFound_throwsException() {
        when(notificationRepository.findById("missing")).thenReturn(java.util.Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.markAsRead("missing", "hadeel"));
    }

    @Test
    void markAsRead_notificationBelongsToOtherUser_throwsException() {
        Notification n = new Notification("other-user", "msg", NotificationType.GENERIC);

        when(notificationRepository.findById("notif-2")).thenReturn(java.util.Optional.of(n));

        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.markAsRead("notif-2", "hadeel"));
    }

    @Test
    void markAsRead_blankNotificationId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.markAsRead("   ", "hadeel"));
    }

    // ── getUnreadCount ────────────────────────────────────────────────────────

    @Test
    void getUnreadCount_returnsCorrectCount() {
        when(notificationRepository.countByRecipientUsernameAndReadFalse("hadeel")).thenReturn(5L);

        long count = notificationsService.getUnreadCount("hadeel");

        assertEquals(5L, count);
        verify(notificationRepository).countByRecipientUsernameAndReadFalse("hadeel");
    }

    @Test
    void getUnreadCount_blankUsername_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.getUnreadCount(""));
    }

    // ── domain-event notify helpers ───────────────────────────────────────────

    @Test
    void notifyPurchaseSuccess_createsNotificationWithCorrectType() {
        notificationsService.notifyPurchaseSuccess("hadeel", "Concert A");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.PURCHASE_SUCCESS
                && n.getRecipientUsername().equals("hadeel")));
    }

    @Test
    void notifyEventCancelled_createsNotificationWithCorrectType() {
        notificationsService.notifyEventCancelled("hadeel", "Concert B");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.EVENT_CANCELLED
                && n.getMessage().contains("Concert B")));
    }

    @Test
    void notifyManagerAppointed_createsNotificationWithCorrectType() {
        notificationsService.notifyManagerAppointed("hadeel", "Acme Corp");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.MANAGER_APPOINTED
                && n.getMessage().contains("Acme Corp")));
    }

    @Test
    void notifyManagerRemoved_createsNotificationWithCorrectType() {
        notificationsService.notifyManagerRemoved("hadeel", "Acme Corp");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.MANAGER_REMOVED));
    }

    @Test
    void notifyLotteryWin_messageContainsAccessCode() {
        notificationsService.notifyLotteryWin("hadeel", "Festival", "CODE-123");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.LOTTERY_WIN
                && n.getMessage().contains("CODE-123")));
    }

    @Test
    void notifyLotteryLoss_createsNotificationWithCorrectType() {
        notificationsService.notifyLotteryLoss("hadeel", "Festival");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.LOTTERY_LOSS
                && n.getMessage().contains("Festival")));
    }

    // ── createNotificationsBatch ──────────────────────────────────────────────

    @Test
    void createNotificationsBatch_emptyList_doesNotSave() {
        notificationsService.createNotificationsBatch(List.of());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void createNotificationsBatch_validList_savesAll() {
        List<Notification> batch = List.of(
                new Notification("user1", "msg1", NotificationType.GENERIC),
                new Notification("user2", "msg2", NotificationType.GENERIC));

        notificationsService.createNotificationsBatch(batch);

        verify(notificationRepository).saveAll(batch);
    }

    // ── constructor ───────────────────────────────────────────────────────────

    @Test
    void constructor_nullRepository_throwsException() {
        assertThrows(NullPointerException.class,
                () -> new NotificationService(null, realtimeNotificationSender));
    }

    @Test
    void constructor_nullNotifier_throwsException() {
        assertThrows(NullPointerException.class,
                () -> new NotificationService(notificationRepository, null));
    }

    // ── catch: real-time delivery fails but notification is still saved ────────

    @Test
    void createNotification_notifierThrows_notificationStillSaved() {
        when(realtimeNotificationSender.notifyUser(anyString(), any()))
                .thenThrow(new RuntimeException("push service down"));

        String id = notificationsService.createNotification(
                "hadeel", "message", NotificationType.GENERIC);

        assertNotNull(id);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_repoThrowsRuntimeException_propagates() {
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> notificationsService.createNotification(
                        "hadeel", "message", NotificationType.GENERIC));
    }

    // ── catch: createNotificationsBatch notifier fails per item ──────────────

    @Test
    void createNotificationsBatch_notifierThrowsForItem_swallowsAndContinues() {
        when(realtimeNotificationSender.notifyUser(anyString(), any()))
                .thenThrow(new RuntimeException("push failed"));

        List<Notification> batch = List.of(
                new Notification("user1", "msg1", NotificationType.GENERIC),
                new Notification("user2", "msg2", NotificationType.GENERIC));

        assertDoesNotThrow(() -> notificationsService.createNotificationsBatch(batch));
        verify(notificationRepository).saveAll(batch);
    }

    // ── catch: repo throws RuntimeException (non-IAE) ─────────────────────────

    @Test
    void getNotificationsForUser_repoThrowsRuntimeException_propagates() {
        when(notificationRepository.findByRecipientUsername("hadeel"))
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(RuntimeException.class,
                () -> notificationsService.getNotificationsForUser("hadeel"));
    }

    @Test
    void getUnreadNotificationsForUser_repoThrowsRuntimeException_propagates() {
        when(notificationRepository.findByRecipientUsernameAndReadFalse("hadeel"))
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(RuntimeException.class,
                () -> notificationsService.getUnreadNotificationsForUser("hadeel"));
    }

    @Test
    void markAsRead_repoThrowsRuntimeException_propagates() {
        when(notificationRepository.findById("notif-x"))
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(RuntimeException.class,
                () -> notificationsService.markAsRead("notif-x", "hadeel"));
    }

    @Test
    void getUnreadCount_repoThrowsRuntimeException_propagates() {
        when(notificationRepository.countByRecipientUsernameAndReadFalse("hadeel"))
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(RuntimeException.class,
                () -> notificationsService.getUnreadCount("hadeel"));
    }

    // ── markAllAsRead ─────────────────────────────────────────────────────────

    @Test
    void markAllAsRead_validUser_marksAllAndSaves() {
        Notification n1 = new Notification("hadeel", "msg1", NotificationType.GENERIC);
        Notification n2 = new Notification("hadeel", "msg2", NotificationType.SYSTEM_ANNOUNCEMENT);

        when(notificationRepository.findByRecipientUsernameAndReadFalse("hadeel"))
                .thenReturn(List.of(n1, n2));

        notificationsService.markAllAsRead("hadeel");

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }

    @Test
    void markAllAsRead_blankUsername_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.markAllAsRead("   "));
    }

    @Test
    void markAllAsRead_repoThrowsRuntimeException_propagates() {
        when(notificationRepository.findByRecipientUsernameAndReadFalse("hadeel"))
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(RuntimeException.class,
                () -> notificationsService.markAllAsRead("hadeel"));
    }

    // ── untested notify helpers ────────────────────────────────────────────────

    @Test
    void notifyEventRescheduled_createsNotificationWithCorrectType() {
        notificationsService.notifyEventRescheduled("hadeel", "Concert C");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.EVENT_RESCHEDULED
                && n.getMessage().contains("Concert C")));
    }

    @Test
    void notifyCompanyClosed_createsNotificationWithCorrectType() {
        notificationsService.notifyCompanyClosed("hadeel", "Acme Corp");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.COMPANY_CLOSED
                && n.getMessage().contains("Acme Corp")));
    }

    @Test
    void notifyCompanyReopened_createsNotificationWithCorrectType() {
        notificationsService.notifyCompanyReopened("hadeel", "Acme Corp");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.COMPANY_REOPENED));
    }

    @Test
    void notifyOwnerAppointed_createsNotificationWithCorrectType() {
        notificationsService.notifyOwnerAppointed("hadeel", "Acme Corp");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.OWNER_APPOINTED
                && n.getMessage().contains("Acme Corp")));
    }

    @Test
    void notifyOwnerRemoved_createsNotificationWithCorrectType() {
        notificationsService.notifyOwnerRemoved("hadeel", "Acme Corp");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.OWNER_REMOVED));
    }

    @Test
    void notifyComplaintResolved_createsNotificationWithCorrectType() {
        notificationsService.notifyComplaintResolved("hadeel", "complaint-42");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.COMPLAINT_RESOLVED
                && n.getMessage().contains("complaint-42")));
    }

    @Test
    void notifyLotteryWin_withNullAccessCode_messageDoesNotContainNull() {
        notificationsService.notifyLotteryWin("hadeel", "Festival", null);
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.LOTTERY_WIN
                && !n.getMessage().contains("null")));
    }

    @Test
    void notifyOrderExpiryWarning_createsNotificationWithCorrectType() {
        notificationsService.notifyOrderExpiryWarning("hadeel", "Concert D");
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.ORDER_EXPIRY_WARNING
                && n.getMessage().contains("Concert D")));
    }
}