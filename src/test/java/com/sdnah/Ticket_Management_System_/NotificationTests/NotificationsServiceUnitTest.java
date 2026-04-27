package com.sdnah.Ticket_Management_System_.NotificationTests;

import com.sdnah.Ticket_Management_System_.Application_Layer.Notifications.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.InMemoryNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class NotificationsServiceUnitTest {

    private NotificationService notificationsService;

    @BeforeEach
    void setUp() {
        notificationsService = new NotificationService(new InMemoryNotificationRepository());
    }


    //create Notification with valid Input -> returns Notification Id
    @Test
    void createNotification_validInput_returnsNotificationId() {
        String id = notificationsService.createNotification("hadeel","Company updated", NotificationType.SYSTEM_ANNOUNCEMENT);

        assertNotNull(id);
        assertFalse(id.isBlank());
    }


    //create Notification then Get Notifications For User -> returns Stored Notification
    @Test
    void createNotification_thenGetNotificationsForUser_returnsStoredNotification() {
        notificationsService.createNotification("hadeel", "You have a new notification", NotificationType.GENERIC);

        List<NotificationDTO> notifications = notificationsService.getNotificationsForUser("hadeel");

        assertEquals(1, notifications.size());
        assertEquals("hadeel", notifications.get(0).getRecipientUsername());
        assertEquals("You have a new notification", notifications.get(0).getMessage());
        assertEquals(NotificationType.GENERIC, notifications.get(0).getType());
    }


    //get Notifications For User -> returns Only Matching User Notifications
    @Test
    void getNotificationsForUser_returnsOnlyMatchingUserNotifications() {
        notificationsService.createNotification("hadeel", "msg1", NotificationType.GENERIC);
        notificationsService.createNotification("ameer", "msg2", NotificationType.GENERIC);
        notificationsService.createNotification("hadeel", "msg3", NotificationType.SYSTEM_ANNOUNCEMENT);

        List<NotificationDTO> notifications = notificationsService.getNotificationsForUser("hadeel");

        assertEquals(2, notifications.size());
        assertTrue(notifications.stream()
                .allMatch(notification -> notification.getRecipientUsername().equals("hadeel")));
    }


    //get Notifications For User when User Has No Notifications -> returns Empty List
    @Test
    void getNotificationsForUser_whenUserHasNoNotifications_returnsEmptyList() {
        List<NotificationDTO> notifications = notificationsService.getNotificationsForUser("unknownUser");

        assertNotNull(notifications);
        assertTrue(notifications.isEmpty());
    }


    //create Notification with Blank Recipient -> throws Exception
    @Test
    void createNotification_withBlankRecipient_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                notificationsService.createNotification("   ", "message", NotificationType.GENERIC));
    }


    //create Notification with Blank Message -> throws Exception
    @Test
    void createNotification_withBlankMessage_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                notificationsService.createNotification("hadeel", "   ", NotificationType.GENERIC));
    }


    //create Notification with Null Type -> throws Exception
    @Test
    void createNotification_withNullType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                notificationsService.createNotification("hadeel", "message", null));
    }
}
