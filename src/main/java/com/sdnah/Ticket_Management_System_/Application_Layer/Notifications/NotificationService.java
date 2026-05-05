package com.sdnah.Ticket_Management_System_.Application_Layer.Notifications;

import com.sdnah.Ticket_Management_System_.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.NotificationRepository;
import org.springframework.stereotype.Service;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.NotificationType;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


@Service
public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    private final NotificationRepository notificationRepository;
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository);
    }

 
    public String createNotification(String recipientUsername, String message, NotificationType type) {
        try {
            Notification notification = new Notification(recipientUsername, message, type);
            notificationRepository.save(notification);

            //event log requirement: log application level events with context
            logger.info("Notification created for recipient=" + recipientUsername + ", type=" + type);

            return notification.getId();
        } catch (IllegalArgumentException e) {
        logger.warning("Invalid notification request: " + e.getMessage());
        throw e;

     } catch (RuntimeException e) {
        logger.log(Level.SEVERE, "System error while creating notification.", e);
        throw e;
    }
    }

    public List<NotificationDTO> getNotificationsForUser(String recipientUsername) {
        try {
            return notificationRepository.findByRecipientUsername(recipientUsername).stream()
                    .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                    .map(NotificationDTO::fromDomain)
                    .toList();

        } catch (IllegalArgumentException e) {
            logger.warning("Invalid notification fetch request: " + e.getMessage());
            throw e;

        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "System error while fetching notifications for user=" + recipientUsername, e);
            throw e;
        }
    }
}