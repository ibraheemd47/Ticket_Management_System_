package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.NotificationRepository;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    private final NotificationRepository notificationRepository;
    private final INotifier notifier;

    public NotificationService(NotificationRepository notificationRepository,
                               INotifier realtimeNotificationSender) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository);
        this.notifier = Objects.requireNonNull(realtimeNotificationSender);
    }

    @Transactional
    public String createNotification(String recipientUsername, String message, NotificationType type) {
        try {
            Notification notification = new Notification(recipientUsername, message, type);
            notificationRepository.save(notification);

           NotificationDTO dto = NotificationDTO.fromDomain(notification);

            try {
                boolean is_notify_send =notifier.notifyUser(recipientUsername.trim(), dto);
                if(is_notify_send){
                    logger.info("Real-time notification sent. recipient=" + recipientUsername + ", type=" + type);
                } else {
                    logger.info("Notification saved but recipient is not connected for real-time delivery. recipient=" +
                            recipientUsername + ", type=" + type);
                }
            } catch (RuntimeException deliveryError) {
                logger.warning("Notification saved but real-time delivery failed. recipient="
                        + recipientUsername + ", reason=" + deliveryError.getMessage());
            }

            logger.info("Notification created and delivery attempted. recipient="
                    + recipientUsername + ", type=" + type);

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
            validateUsername(recipientUsername);

            return notificationRepository.findByRecipientUsername(recipientUsername.trim()).stream()
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

    public List<NotificationDTO> getUnreadNotificationsForUser(String recipientUsername) {
        try {
            validateUsername(recipientUsername);

            return notificationRepository.findByRecipientUsernameAndReadFalse(recipientUsername.trim()).stream()
                    .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                    .map(NotificationDTO::fromDomain)
                    .toList();

        } catch (IllegalArgumentException e) {
            logger.warning("Invalid unread notification fetch request: " + e.getMessage());
            throw e;

        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "System error while fetching unread notifications for user=" + recipientUsername, e);
            throw e;
        }
    }

    @Transactional
    public void markAsRead(String notificationId, String recipientUsername) {
        try {
            validateNotificationId(notificationId);
            validateUsername(recipientUsername);

            Notification notification = notificationRepository.findById(notificationId.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Notification not found."));

            if (!notification.belongsTo(recipientUsername)) {
                throw new IllegalArgumentException("Notification does not belong to this user.");
            }

            notification.markAsRead();
            notificationRepository.save(notification);

            logger.info("Notification marked as read. id=" + notificationId + ", user=" + recipientUsername);

        } catch (IllegalArgumentException e) {
            logger.warning("Invalid mark-as-read request: " + e.getMessage());
            throw e;

        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "System error while marking notification as read.", e);
            throw e;
        }
    }

    public String 
    notifyPurchaseSuccess(String recipientUsername, String eventName) {
        return createNotification(
                recipientUsername,
                "Purchase completed successfully for event: " + eventName,
                NotificationType.PURCHASE_SUCCESS
        );
    }

    public String notifyEventCancelled(String recipientUsername, String eventName) {
        return createNotification(
                recipientUsername,
                "Event cancelled: " + eventName,
                NotificationType.EVENT_CANCELLED
        );
    }

    public String notifyEventRescheduled(String recipientUsername, String eventName) {
        return createNotification(
                recipientUsername,
                "Event rescheduled: " + eventName,
                NotificationType.EVENT_RESCHEDULED
        );
    }

    public String notifyOrderExpiryWarning(String recipientUsername, String eventName) {
        return createNotification(
                recipientUsername,
                "Your reserved tickets for event '" + eventName + "' are about to expire.",
                NotificationType.ORDER_EXPIRY_WARNING
        );
    }

    public String notifyCompanyClosed(String recipientUsername, String companyName) {
        return createNotification(
                recipientUsername,
                "Company closed: " + companyName,
                NotificationType.COMPANY_CLOSED
        );
    }

    public String notifyCompanyReopened(String recipientUsername, String companyName) {
        return createNotification(
                recipientUsername,
                "Company reopened: " + companyName,
                NotificationType.COMPANY_REOPENED
        );
    }

    public String notifyRoleChanged(String recipientUsername, String companyName) {
        return createNotification(
                recipientUsername,
                "Your role was changed in company: " + companyName,
                NotificationType.ROLE_CHANGED
        );
    }

    public String notifyManagerAppointed(String recipientUsername, String companyName) {
        return createNotification(
                recipientUsername,
                "You were appointed as manager in company: " + companyName,
                NotificationType.MANAGER_APPOINTED
        );
    }

    public String notifyManagerRemoved(String recipientUsername, String companyName) {
        return createNotification(
                recipientUsername,
                "You were removed from manager role in company: " + companyName,
                NotificationType.MANAGER_REMOVED
        );
    }

    public String notifyPermissionsChanged(String recipientUsername, String companyName) {
        return createNotification(
                recipientUsername,
                "Your permissions were changed in company: " + companyName,
                NotificationType.PERMISSIONS_CHANGED
        );
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Recipient username cannot be null or blank.");
        }
    }

    private void validateNotificationId(String notificationId) {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("Notification id cannot be null or blank.");
        }
    }

    public long getUnreadCount(String recipientUsername) {
        try {
            validateUsername(recipientUsername);
            return notificationRepository
                    .countByRecipientUsernameAndReadFalse(recipientUsername.trim());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid unread-count request: " + e.getMessage());
            throw e;

        } catch (RuntimeException e) {
            logger.log(Level.SEVERE,
                    "System error while counting unread notifications for user="
                            + recipientUsername,
                    e);
            throw e;
        }
    }

    @Transactional
    public void markAllAsRead(String recipientUsername) {
        try {
            validateUsername(recipientUsername);
            List<Notification> notifications =
                    notificationRepository.findByRecipientUsernameAndReadFalse(recipientUsername.trim());
            for (Notification notification : notifications) {
                notification.markAsRead();
            }
            notificationRepository.saveAll(notifications);
            logger.info("All notifications marked as read for user=" + recipientUsername);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid mark-all-as-read request: " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE,
                    "System error while marking all notifications as read for user="
                            + recipientUsername,
                    e);
            throw e;
        }
    }

    public String notifyOwnerAppointed(String recipientUsername, String companyName) {
        return createNotification(
                recipientUsername,
                "You were appointed as owner in company: " + companyName,
                NotificationType.OWNER_APPOINTED
        );
    }

    public String notifyOwnerRemoved(String recipientUsername, String companyName) {
        return createNotification(
                recipientUsername,
                "You were removed from owner role in company: " + companyName,
                NotificationType.OWNER_REMOVED
        );
    }
}