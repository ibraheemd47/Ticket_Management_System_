package com.sdnah.Ticket_Management_System_.NotificationTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.NotificationRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceIntegrationTest {

    private static final String USER_A = "alice";
    private static final String USER_B = "bob";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("Given valid input, when creating notification, then it is persisted with id")
    void givenValidInput_WhenCreatingNotification_ThenItIsPersistedWithId() {
        String id = notificationService.createNotification(USER_A, "Welcome", NotificationType.GENERIC);

        assertNotNull(id);
        assertTrue(notificationRepository.findById(id).isPresent());
    }

    @Test
    @DisplayName("Given user with notifications, when fetching, then DTOs are returned")
    void givenUserWithNotifications_WhenFetching_ThenDTOsAreReturned() {
        notificationService.createNotification(USER_A, "Hello", NotificationType.GENERIC);
        notificationService.createNotification(USER_A, "Purchase confirmed", NotificationType.PURCHASE_SUCCESS);

        List<NotificationDTO> result = notificationService.getNotificationsForUser(USER_A);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(n -> USER_A.equals(n.getRecipientUsername())));
    }

    @Test
    @DisplayName("Given multiple users with notifications, when fetching for one, then only theirs is returned")
    void givenMultipleUsersWithNotifications_WhenFetchingForOne_ThenOnlyTheirsIsReturned() {
        notificationService.createNotification(USER_A, "For Alice", NotificationType.GENERIC);
        notificationService.createNotification(USER_B, "For Bob", NotificationType.GENERIC);
        notificationService.createNotification(USER_B, "For Bob again", NotificationType.SYSTEM_ANNOUNCEMENT);

        List<NotificationDTO> aliceResults = notificationService.getNotificationsForUser(USER_A);
        List<NotificationDTO> bobResults = notificationService.getNotificationsForUser(USER_B);

        assertEquals(1, aliceResults.size());
        assertEquals(2, bobResults.size());
    }

    @Test
    @DisplayName("Given notifications created in order, when fetching, then results are sorted descending by createdAt")
    void givenNotificationsCreatedInOrder_WhenFetching_ThenResultsAreSortedDescending() throws InterruptedException {
        notificationService.createNotification(USER_A, "First", NotificationType.GENERIC);
        Thread.sleep(5);
        notificationService.createNotification(USER_A, "Second", NotificationType.GENERIC);
        Thread.sleep(5);
        notificationService.createNotification(USER_A, "Third", NotificationType.GENERIC);

        List<NotificationDTO> result = notificationService.getNotificationsForUser(USER_A);

        assertEquals(3, result.size());
        assertEquals("Third", result.get(0).getMessage());
        assertEquals("Second", result.get(1).getMessage());
        assertEquals("First", result.get(2).getMessage());
    }

    @Test
    @DisplayName("Given user with no notifications, when fetching, then empty list is returned")
    void givenUserWithNoNotifications_WhenFetching_ThenEmptyListIsReturned() {
        List<NotificationDTO> result = notificationService.getNotificationsForUser(USER_A);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Given blank message, when creating notification, then IllegalArgumentException is thrown")
    void givenBlankMessage_WhenCreatingNotification_ThenIllegalArgumentExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.createNotification(USER_A, "  ", NotificationType.GENERIC));
    }

    @Test
    @DisplayName("Given blank recipient, when creating notification, then IllegalArgumentException is thrown")
    void givenBlankRecipient_WhenCreatingNotification_ThenIllegalArgumentExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.createNotification("", "Hello", NotificationType.GENERIC));
    }

    @Test
    @DisplayName("Given null type, when creating notification, then IllegalArgumentException is thrown")
    void givenNullType_WhenCreatingNotification_ThenIllegalArgumentExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.createNotification(USER_A, "Hello", null));
    }
}