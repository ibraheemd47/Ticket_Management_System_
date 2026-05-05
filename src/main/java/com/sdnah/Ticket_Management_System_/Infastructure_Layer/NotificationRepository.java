package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByRecipientUsername(String recipientUsername);
}