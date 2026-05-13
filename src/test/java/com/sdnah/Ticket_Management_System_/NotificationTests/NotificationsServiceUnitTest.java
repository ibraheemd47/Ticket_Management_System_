package com.sdnah.Ticket_Management_System_.NotificationTests;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.RealtimeNotificationSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NotificationsServiceUnitTest {

    private NotificationService notificationsService;
    private NotificationRepository notificationRepository;
    private RealtimeNotificationSender realtimeNotificationSender;

    @BeforeEach
        void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        realtimeNotificationSender = mock(RealtimeNotificationSender.class);

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
}