package com.sdnah.Ticket_Management_System_.NotificationTests;

import com.sdnah.Ticket_Management_System_.Application_Layer.Notifications.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.InMemoryNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationsAcceptanceTest {

    private NotificationService notificationsService;

    @BeforeEach
    void setUp() {
        notificationsService = new NotificationService(new InMemoryNotificationRepository());
    }


    //create Notification for Member: notification Is Stored And Retrievable
    @Test
    void createNotification_forMember_notificationIsStoredAndRetrievable() {
        String notificationId = notificationsService.createNotification("hadeel", "You have a new notification",
        NotificationType.GENERIC);

        List<NotificationDTO> notifications = notificationsService.getNotificationsForUser("hadeel");

        assertNotNull(notificationId);
        assertEquals(1, notifications.size());
        assertEquals("hadeel", notifications.get(0).getRecipientUsername());
        assertEquals("You have a new notification", notifications.get(0).getMessage());
    }


    //create Multiple Notifications for Different Users -> each User Gets Own Notifications
    @Test
    void createMultipleNotifications_forDifferentUsers_eachUserGetsOwnNotifications() {
        notificationsService.createNotification("hadeel", "msg1", NotificationType.GENERIC);
        notificationsService.createNotification("ameer", "msg2", NotificationType.SYSTEM_ANNOUNCEMENT);
        notificationsService.createNotification("hadeel", "msg3", NotificationType.GENERIC);

        List<NotificationDTO> hadeelNotifications = notificationsService.getNotificationsForUser("hadeel");
        List<NotificationDTO> ameerNotifications = notificationsService.getNotificationsForUser("ameer");

        assertEquals(2, hadeelNotifications.size());
        assertEquals(1, ameerNotifications.size());
        assertTrue(hadeelNotifications.stream().allMatch(n -> n.getRecipientUsername().equals("hadeel")));
        assertTrue(ameerNotifications.stream().allMatch(n -> n.getRecipientUsername().equals("ameer")));
    }


    //create Notification with Invalid Input -> request Fails
    @Test
    void createNotification_withInvalidInput_requestFails() {
        assertThrows(IllegalArgumentException.class, () ->
                notificationsService.createNotification("", "message", NotificationType.GENERIC));
    }


    //get Notifications For User when No Notifications Exist -> returns Empty List
    @Test
    void getNotificationsForUser_whenNoNotificationsExist_returnsEmptyList() {
        List<NotificationDTO> notifications = notificationsService.getNotificationsForUser("noUser");

        assertNotNull(notifications);
        assertTrue(notifications.isEmpty());
    }
}
