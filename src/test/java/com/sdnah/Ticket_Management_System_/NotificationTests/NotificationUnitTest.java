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

    // ── markAsRead / isRead ───────────────────────────────────────────────────

    @Test
    void isRead_initiallyFalse() {
        Notification notification = new Notification("hadeel", "message", NotificationType.GENERIC);
        assertFalse(notification.isRead());
    }

    @Test
    void markAsRead_setsReadToTrue() {
        Notification notification = new Notification("hadeel", "message", NotificationType.GENERIC);
        notification.markAsRead();
        assertTrue(notification.isRead());
    }

    @Test
    void markAsRead_calledTwice_remainsTrue() {
        Notification notification = new Notification("hadeel", "message", NotificationType.GENERIC);
        notification.markAsRead();
        notification.markAsRead();
        assertTrue(notification.isRead());
    }

    // ── ID uniqueness / createdAt ─────────────────────────────────────────────

    @Test
    void twoNotifications_haveUniqueIds() {
        Notification n1 = new Notification("hadeel", "msg1", NotificationType.GENERIC);
        Notification n2 = new Notification("hadeel", "msg2", NotificationType.GENERIC);
        assertNotEquals(n1.getId(), n2.getId());
    }

    @Test
    void createdAt_isNotNull() {
        Notification notification = new Notification("hadeel", "message", NotificationType.GENERIC);
        assertNotNull(notification.getCreatedAt());
    }

    // ── null inputs ───────────────────────────────────────────────────────────

    @Test
    void createNotification_withNullRecipient_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification(null, "message", NotificationType.GENERIC));
    }

    @Test
    void createNotification_withNullMessage_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("hadeel", null, NotificationType.GENERIC));
    }

    // ── belongsTo edge cases ──────────────────────────────────────────────────

    @Test
    void belongsTo_withNullUsername_returnsFalse() {
        Notification notification = new Notification("hadeel", "message", NotificationType.GENERIC);
        assertFalse(notification.belongsTo(null));
    }

    @Test
    void belongsTo_withBlankUsername_returnsFalse() {
        Notification notification = new Notification("hadeel", "message", NotificationType.GENERIC);
        assertFalse(notification.belongsTo("   "));
    }

    @Test
    void belongsTo_recipientWithLeadingSpaces_matchesAfterTrim() {
        Notification notification = new Notification("  hadeel  ", "message", NotificationType.GENERIC);
        assertTrue(notification.belongsTo("hadeel"));
    }

    // ── full constructor ──────────────────────────────────────────────────────

    @Test
    void fullConstructor_setsReadFlag() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Notification notification = new Notification("id-1", "hadeel", "message",
                NotificationType.GENERIC, now, true);
        assertTrue(notification.isRead());
        assertEquals("id-1", notification.getId());
        assertEquals(now, notification.getCreatedAt());
    }

    @Test
    void fullConstructor_withBlankId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("   ", "hadeel", "message",
                        NotificationType.GENERIC, java.time.LocalDateTime.now(), false));
    }

    @Test
    void fullConstructor_withNullCreatedAt_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Notification("id-1", "hadeel", "message",
                        NotificationType.GENERIC, null, false));
    }

    // ── equals / hashCode ─────────────────────────────────────────────────────

    @Test
    void twoNotificationsWithSameId_areEqual() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Notification n1 = new Notification("same-id", "hadeel", "msg1",
                NotificationType.GENERIC, now, false);
        Notification n2 = new Notification("same-id", "hadeel", "msg2",
                NotificationType.SYSTEM_ANNOUNCEMENT, now, true);
        assertEquals(n1, n2);
        assertEquals(n1.hashCode(), n2.hashCode());
    }

    @Test
    void twoNotificationsWithDifferentIds_areNotEqual() {
        Notification n1 = new Notification("hadeel", "msg1", NotificationType.GENERIC);
        Notification n2 = new Notification("hadeel", "msg2", NotificationType.GENERIC);
        assertNotEquals(n1, n2);
    }
}
