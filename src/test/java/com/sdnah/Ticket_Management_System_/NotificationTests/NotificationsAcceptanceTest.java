package com.sdnah.Ticket_Management_System_.NotificationTests;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.INotifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class NotificationsAcceptanceTest {

    private NotificationService notificationsService;
    private NotificationRepository notificationRepository;
    private Map<String, Notification> notifications;
    private INotifier realtimeNotificationSender;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        realtimeNotificationSender = mock(INotifier.class);
        notifications = new ConcurrentHashMap<>();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notifications.put(notification.getId(), notification);
            return notification;
        });

        when(notificationRepository.findByRecipientUsername(anyString())).thenAnswer(invocation -> {
            String username = invocation.getArgument(0);
            return notifications.values().stream()
                    .filter(notification -> notification.getRecipientUsername().equals(username))
                    .toList();
        });

        notificationsService = new NotificationService(
        notificationRepository,
        realtimeNotificationSender
);
    }

    @Test
    void createNotification_forMember_notificationIsStoredAndRetrievable() {
        String notificationId = notificationsService.createNotification(
                "hadeel",
                "You have a new notification",
                NotificationType.GENERIC
        );

        List<NotificationDTO> notifications =
                notificationsService.getNotificationsForUser("hadeel");

        assertNotNull(notificationId);
        assertEquals(1, notifications.size());
        assertEquals("hadeel", notifications.get(0).getRecipientUsername());
        assertEquals("You have a new notification", notifications.get(0).getMessage());
    }

    @Test
    void createMultipleNotifications_forDifferentUsers_eachUserGetsOwnNotifications() {
        notificationsService.createNotification("hadeel", "msg1", NotificationType.GENERIC);
        notificationsService.createNotification("ameer", "msg2", NotificationType.SYSTEM_ANNOUNCEMENT);
        notificationsService.createNotification("hadeel", "msg3", NotificationType.GENERIC);

        List<NotificationDTO> hadeelNotifications =
                notificationsService.getNotificationsForUser("hadeel");
        List<NotificationDTO> ameerNotifications =
                notificationsService.getNotificationsForUser("ameer");

        assertEquals(2, hadeelNotifications.size());
        assertEquals(1, ameerNotifications.size());
        assertTrue(hadeelNotifications.stream()
                .allMatch(n -> n.getRecipientUsername().equals("hadeel")));
        assertTrue(ameerNotifications.stream()
                .allMatch(n -> n.getRecipientUsername().equals("ameer")));
    }

    @Test
    void createNotification_withInvalidInput_requestFails() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationsService.createNotification("", "message", NotificationType.GENERIC));
    }

    @Test
    void getNotificationsForUser_whenNoNotificationsExist_returnsEmptyList() {
        List<NotificationDTO> notifications =
                notificationsService.getNotificationsForUser("noUser");

        assertNotNull(notifications);
        assertTrue(notifications.isEmpty());
    }
}