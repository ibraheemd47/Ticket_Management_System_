package com.sdnah.Ticket_Management_System_.NotificationTests;

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;

import static org.junit.jupiter.api.Assertions.*;


public class NotificationUnitTest {

    //with Valid Input -> success
    @Test
    void createNotification_withValidInput_success() {
        Notification notification = new Notification("hadeel", "You have a new notification", NotificationType.GENERIC);

        assertNotNull(notification.getId());
        assertEquals("hadeel", notification.getRecipientUsername());
        assertEquals("You have a new notification", notification.getMessage());
        assertEquals(NotificationType.GENERIC, notification.getType());
        assertNotNull(notification.getCreatedAt());
    }

    //with Blank Recipient -> fail
    @Test
    void createNotification_withBlankRecipient_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("   ", "message", NotificationType.GENERIC));
    }

    //with Blank Message -> fail
    @Test
    void createNotification_withBlankMessage_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("hadeel", "   ", NotificationType.GENERIC));
    }

    //with Null Type -> fail
    @Test
    void createNotification_withNullType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("hadeel", "message", null));
    }


    //sent to same Recipient -> true
    @Test
    void belongsTo_sameRecipient_returnsTrue() {
        Notification notification = new Notification( "hadeel", "message", NotificationType.GENERIC);

        assertTrue(notification.belongsTo("hadeel"));
    }


    //send to different Recipient -> false
    @Test
    void belongsTo_differentRecipient_returnsFalse() {
        Notification notification = new Notification( "hadeel", "message",NotificationType.GENERIC);

        assertFalse(notification.belongsTo("ameer"));
    }
}
