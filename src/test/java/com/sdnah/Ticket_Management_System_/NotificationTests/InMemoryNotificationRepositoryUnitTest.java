package com.sdnah.Ticket_Management_System_.NotificationTests;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.InMemoryNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


public class InMemoryNotificationRepositoryUnitTest {

    private InMemoryNotificationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepository();
    }


    //save and Find By Id -> success
    @Test
    void save_andFindById_success() {
        Notification notification = new Notification("hadeel", "message", NotificationType.GENERIC);

        repository.save(notification);

        Optional<Notification> result = repository.findById(notification.getId());

        assertTrue(result.isPresent());
        assertEquals(notification.getId(), result.get().getId());
    }


    //find By Recipient Username -> returns Only Matching Notifications
    @Test
    void findByRecipientUsername_returnsOnlyMatchingNotifications() {
        Notification n1 = new Notification("hadeel", "msg1", NotificationType.GENERIC);
        Notification n2 = new Notification("hadeel", "msg2", NotificationType.SYSTEM_ANNOUNCEMENT);
        Notification n3 = new Notification("ameer", "msg3", NotificationType.GENERIC);

        repository.save(n1);
        repository.save(n2);
        repository.save(n3);

        List<Notification> result = repository.findByRecipientUsername("hadeel");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(n -> n.getRecipientUsername().equals("hadeel")));
    }


    //find All -> returns All Notifications
    @Test
    void findAll_returnsAllNotifications() {
        repository.save(new Notification("u1", "msg1", NotificationType.GENERIC));
        repository.save(new Notification("u2", "msg2", NotificationType.GENERIC));

        List<Notification> result = repository.findAll();

        assertEquals(2, result.size());
    }


    //clear -> removes All Notifications
    @Test
    void clear_removesAllNotifications() {
        repository.save(new Notification("u1", "msg1", NotificationType.GENERIC));
        repository.save(new Notification("u2", "msg2", NotificationType.GENERIC));

        repository.clear();

        assertEquals(0, repository.findAll().size());
    }


    //save null Notification -> throws Exception
    @Test
    void save_nullNotification_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }
}
