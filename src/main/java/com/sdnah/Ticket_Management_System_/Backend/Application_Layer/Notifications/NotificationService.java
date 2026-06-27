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

    /**
     * Persists all notifications in a single batch INSERT, then pushes
     * real-time delivery for each one. Avoids N individual DB round-trips
     * when notifying all lottery participants at once.
     */
    @Transactional
    public void createNotificationsBatch(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) return;
        // Single batch INSERT for all rows
        notificationRepository.saveAll(notifications);
        // Real-time push per user (non-blocking; failures are logged, not thrown)
        for (Notification n : notifications) {
            try {
                notifier.notifyUser(n.getRecipientUsername(), NotificationDTO.fromDomain(n));
            } catch (Exception ex) {
                logger.warning("Real-time push failed for recipient=" + n.getRecipientUsername()
                        + ": " + ex.getMessage());
            }
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

    public String notifyEventVenueChanged(String recipientUsername,
                                          String eventName,
                                          String newVenue) {
        String body = "Venue changed for event '" + eventName + "'"
                + (newVenue == null || newVenue.isBlank() ? "." : " — new venue: " + newVenue);
        return createNotification(recipientUsername, body, NotificationType.EVENT_VENUE_CHANGED);
    }

    public String notifyEventPriceChanged(String recipientUsername,
                                          String eventName,
                                          String ticketType) {
        String body = "Ticket price changed for event '" + eventName + "'"
                + (ticketType == null || ticketType.isBlank() ? "." : " (" + ticketType + ").");
        return createNotification(recipientUsername, body, NotificationType.EVENT_PRICE_CHANGED);
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

    public String notifyManagerAppointmentRequested(String recipientUsername,
                                                    String companyName,
                                                    String appointerName) {
        return createNotification(
                recipientUsername,
                appointerName + " invited you to become a manager of '" + companyName + "'. "
                        + "Open the company chooser to accept or decline.",
                NotificationType.MANAGER_APPOINTMENT_REQUESTED
        );
    }

    public String notifyManagerAppointmentResponded(String recipientUsername,
                                                    String companyName,
                                                    String candidateName,
                                                    boolean accepted) {
        return createNotification(
                recipientUsername,
                candidateName + (accepted ? " accepted" : " declined")
                        + " your manager invitation for '" + companyName + "'.",
                NotificationType.MANAGER_APPOINTMENT_RESPONDED
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

    public String notifyOwnerAppointmentRequested(String recipientUsername,
                                                  String companyName,
                                                  String appointerName) {
        return createNotification(
                recipientUsername,
                appointerName + " invited you to become an owner of '" + companyName + "'. "
                        + "Open the company chooser to accept or decline.",
                NotificationType.OWNER_APPOINTMENT_REQUESTED
        );
    }

    public String notifyOwnerAppointmentResponded(String recipientUsername,
                                                  String companyName,
                                                  String candidateName,
                                                  boolean accepted) {
        return createNotification(
                recipientUsername,
                candidateName + (accepted ? " accepted" : " declined")
                        + " your owner invitation for '" + companyName + "'.",
                NotificationType.OWNER_APPOINTMENT_RESPONDED
        );
    }

    public String notifyOwnerRemoved(String recipientUsername, String companyName) {
        return createNotification(
                recipientUsername,
                "You were removed from owner role in company: " + companyName,
                NotificationType.OWNER_REMOVED
        );
    }
    public String notifyComplaintResolved(String recipientUsername, String complaintId) {
        return createNotification(
                recipientUsername,
                "Your complaint with ID " + complaintId + " has been resolved.",
                NotificationType.COMPLAINT_RESOLVED
        );
    }

    public String notifyLotteryWin(String recipientUsername, String eventName, String accessCode) {
        String message = "🎉 Congratulations! You won the lottery for event: " + eventName + "."
                + (accessCode != null && !accessCode.isBlank()
                   ? " Your access code is: " + accessCode + ". Use it to purchase your ticket."
                   : " You may now purchase your ticket.");
        return createNotification(recipientUsername, message, NotificationType.LOTTERY_WIN);
    }

    public String notifyLotteryLoss(String recipientUsername, String eventName) {
        return createNotification(
                recipientUsername,
                "Unfortunately, you were not selected in the lottery for event: " + eventName
                        + ". Better luck next time!",
                NotificationType.LOTTERY_LOSS);
    }
}