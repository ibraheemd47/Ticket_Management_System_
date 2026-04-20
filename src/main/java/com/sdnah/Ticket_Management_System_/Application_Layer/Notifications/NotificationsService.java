package com.sdnah.Ticket_Management_System_.Application_Layer.Notifications;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.INotificationRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.NotificationType;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


public class NotificationsService {

    private static final Logger logger = Logger.getLogger(NotificationsService.class.getName());

    private final INotificationRepository notificationRepository;

    public NotificationsService(INotificationRepository notificationRepository) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository);
    }

 
    public String createNotification(String recipientUsername, String message, NotificationType type) {
        try {
            Notification notification = new Notification(recipientUsername, message, type);
            notificationRepository.save(notification);

            //event log requirement: log application level events with context
            logger.info("Notification created for recipient=" + recipientUsername + ", type=" + type);

            return notification.getId();
        } catch (RuntimeException e) {
            //error log requirement: log system errors, not business negative scenarios only
            logger.log(Level.SEVERE, "Failed to create notification.", e);
            throw e;
        }
    }

    public List<NotificationDTO> getNotificationsForUser(String recipientUsername) {
        try {
            return notificationRepository.findByRecipientUsername(recipientUsername).stream()
                    .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                    .map(NotificationDTO::fromDomain)
                    .toList();
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to fetch notifications for user=" + recipientUsername, e);
            throw e;
        }
    }
}