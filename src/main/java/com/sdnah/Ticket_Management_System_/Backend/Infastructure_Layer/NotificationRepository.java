package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByRecipientUsername(String recipientUsername);

    List<Notification> findByRecipientUsernameAndReadFalse(String recipientUsername);

    long countByRecipientUsernameAndReadFalse(String recipientUsername);
}