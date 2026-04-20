package com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications;

import java.util.List;
import java.util.Optional;

public interface INotificationRepository {
    void save(Notification notification);

    void saveAll(List<Notification> notifications);

    Optional<Notification> findById(String notificationId);

    List<Notification> findByRecipientUsername(String recipientUsername);

    List<Notification> findActiveByRecipientUsername(String recipientUsername);

    List<Notification> findAll();

    void deleteById(String notificationId);

    void clear();
}
