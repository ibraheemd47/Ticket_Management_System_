package com.sdnah.Ticket_Management_System_.NotificationTests;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Unit tests for the Notification domain entity.
 *
 * Covered behavior:
 * - Valid notification creation
 * - Input validation
 * - Recipient ownership logic
 *
 * Version 1 scope:
 * Only modeled notification behavior is tested here.
 * Real-time and delayed delivery are not implemented in this version.
 */
public class NotificationUnitTest {

    @Test
    void createNotification_withValidInput_success() {
        Notification notification = new Notification(
                "hadeel",
                "You have a new notification",
                NotificationType.GENERIC
        );

        assertNotNull(notification.getId());
        assertEquals("hadeel", notification.getRecipientUsername());
        assertEquals("You have a new notification", notification.getMessage());
        assertEquals(NotificationType.GENERIC, notification.getType());
        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void createNotification_withBlankRecipient_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("   ", "message", NotificationType.GENERIC)
        );
    }

    @Test
    void createNotification_withBlankMessage_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("hadeel", "   ", NotificationType.GENERIC)
        );
    }

    @Test
    void createNotification_withNullType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("hadeel", "message", null)
        );
    }

    @Test
    void belongsTo_sameRecipient_returnsTrue() {
        Notification notification = new Notification(
                "hadeel",
                "message",
                NotificationType.GENERIC
        );

        assertTrue(notification.belongsTo("hadeel"));
    }

    @Test
    void belongsTo_differentRecipient_returnsFalse() {
        Notification notification = new Notification(
                "hadeel",
                "message",
                NotificationType.GENERIC
        );

        assertFalse(notification.belongsTo("ameer"));
    }
}
