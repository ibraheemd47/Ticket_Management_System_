// package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

// import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
// import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
// import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
// import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
// import com.sdnah.Ticket_Management_System_.Frontend.MainView;
// import com.sdnah.Ticket_Management_System_.Frontend.NotificationBell;
// import com.vaadin.flow.spring.annotation.UIScope;
// import org.springframework.stereotype.Component;

// import java.util.List;

// @Component
// @UIScope
// public class NotificationBellPresenter {

//     private final NotificationService notificationService;
//     private final IrepresnteUserService userService;
//     private NotificationBell view;
//     private MainView mainView;

//     public NotificationBellPresenter(NotificationService notificationService, IrepresnteUserService userService) {
//         this.notificationService = notificationService;
//         this.userService = userService;
//     }

//     public void setView(NotificationBell view) {
//         this.view = view;
//     }

//     // Helper to resolve user ID from the token
//     private String resolveMemberId(String token) {
//         if (token == null || token.isBlank()) {
//             return null;
//         }
//         try {
//             String memberId = userService.requireMemberId(token);
//             return (memberId != null && !memberId.isBlank()) ? memberId : null;
//         } catch (Exception e) {
//             return null;
//         }
//     }

//      public NotificationBell CreateNotificationBell() {
//         NotificationBell notifcation_bell = new NotificationBell(this);
//       return notifcation_bell;
//     }

//     public void initAndRefresh(String token) {
//         String memberId = resolveMemberId(token);
//         if (memberId == null) {
//             view.hideBell();
//             return;
//         }

//         view.showBell();
//         updateBadge(memberId);

//         if (view.isDialogOpen()) {
//             loadNotifications(memberId);
//         }
//     }

//     public void openDialogAndLoadNotifications(String token) {
//         String memberId = resolveMemberId(token);
//         if (memberId == null) {
//             view.showToastMessage("Please login first.");
//             return;
//         }

//         updateBadge(memberId);
//         loadNotifications(memberId);
//         view.openDialog();
//     }

//     public void markAllAsRead(String token) {
//         String memberId = resolveMemberId(token);
//         if (memberId != null) {
//             notificationService.markAllAsRead(memberId);
//             updateBadge(memberId);
//             loadNotifications(memberId);
//         }
//     }

//     public void markSingleAsRead(String token, String notificationId) {
//         String memberId = resolveMemberId(token);
//         if (memberId != null) {
//             notificationService.markAsRead(notificationId, memberId);
//             updateBadge(memberId);
//             loadNotifications(memberId);
//         }
//     }

//     private void updateBadge(String memberId) {
//         long unreadCount = notificationService.getUnreadCount(memberId);
//         view.updateBadgeCount(unreadCount);
//     }

//     private void loadNotifications(String memberId) {
//         List<NotificationDTO> unreadNotifications = notificationService.getUnreadNotificationsForUser(memberId);
//         if (unreadNotifications.isEmpty()) {
//             view.showEmptyState("No unread notifications.");
//         } else {
//             view.displayNotifications(unreadNotifications);
//         }
//     }

//     public void setView(MainView mainView) {
//        this.mainView = mainView;
//     }

//     public void createNotification(String memberId, String string, NotificationType roleChanged) {
//         try {
//         notificationService.createNotification(memberId, string, roleChanged);
//         } catch (Exception ex) {
//             view.showToastMessage("Failed to create notification: " + ex.getMessage());
//         }
//     }

//     public void refreshUnreadCount() {
//         try {
//             String token = mainView.getCurrentToken();
//             String memberId = resolveMemberId(token);
//             if (memberId != null) {
//                 updateBadge(memberId);
//             }
//         } catch (Exception ex) {
//             view.showToastMessage("Failed to refresh notifications: " + ex.getMessage());
//         }
//     }
// }

package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Frontend.MainView;
import com.sdnah.Ticket_Management_System_.Frontend.NotificationBell;
import com.vaadin.flow.spring.annotation.UIScope;

@Component
@UIScope
public class NotificationBellPresenter {

    private static final String CONNECTION_ERROR_MESSAGE =
            "The system is temporarily unavailable. "
                    + "Please check your network connection and try again.";

    private final NotificationService notificationService;
    private final IrepresnteUserService userService;

    private NotificationBell view;
    private MainView mainView;

    /*
     * Prevents the same message from appearing every 3 seconds
     * while the database is unavailable.
     */
    private boolean connectionMessageShown = false;

    public NotificationBellPresenter(
            NotificationService notificationService,
            IrepresnteUserService userService) {

        this.notificationService = notificationService;
        this.userService = userService;
    }

    public void setView(NotificationBell view) {
        this.view = view;
    }

    public void setView(MainView mainView) {
        this.mainView = mainView;
    }

    public NotificationBell CreateNotificationBell() {
        return new NotificationBell(this);
    }

    private String resolveMemberId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String memberId = userService.requireMemberId(token);

        if (memberId == null || memberId.isBlank()) {
            return null;
        }

        return memberId;
    }

    /*
     * Called automatically when the bell is attached and every
     * three seconds afterward.
     */
    public void initAndRefresh(String token) {
        try {
            String memberId = resolveMemberId(token);

            if (memberId == null) {
                view.hideBell();
                return;
            }

            view.showBell();
            updateBadge(memberId);

            if (view.isDialogOpen()) {
                loadNotifications(memberId);
            }

            connectionMessageShown = false;

        } catch (Exception exception) {
            showConnectionFailure();
        }
    }

    public void openDialogAndLoadNotifications(String token) {
        try {
            String memberId = resolveMemberId(token);

            if (memberId == null) {
                view.showToastMessage("Please login first.");
                return;
            }

            updateBadge(memberId);
            loadNotifications(memberId);
            view.openDialog();

            connectionMessageShown = false;

        } catch (Exception exception) {
            showConnectionFailure();
        }
    }

    public void markAllAsRead(String token) {
        try {
            String memberId = resolveMemberId(token);

            if (memberId == null) {
                return;
            }

            notificationService.markAllAsRead(memberId);
            updateBadge(memberId);
            loadNotifications(memberId);

            connectionMessageShown = false;

        } catch (Exception exception) {
            showConnectionFailure();
        }
    }

    public void markSingleAsRead(
            String token,
            String notificationId) {

        try {
            String memberId = resolveMemberId(token);

            if (memberId == null) {
                return;
            }

            notificationService.markAsRead(
                    notificationId,
                    memberId
            );

            updateBadge(memberId);
            loadNotifications(memberId);

            connectionMessageShown = false;

        } catch (Exception exception) {
            showConnectionFailure();
        }
    }

    private void updateBadge(String memberId) {
        long unreadCount =
                notificationService.getUnreadCount(memberId);

        view.updateBadgeCount(unreadCount);
    }

    private void loadNotifications(String memberId) {
        List<NotificationDTO> notifications =
                notificationService
                        .getUnreadNotificationsForUser(memberId);

        if (notifications.isEmpty()) {
            view.showEmptyState(
                    "No unread notifications."
            );
        } else {
            view.displayNotifications(notifications);
        }
    }

    public void createNotification(
            String memberId,
            String message,
            NotificationType type) {

        try {
            notificationService.createNotification(
                    memberId,
                    message,
                    type
            );

            connectionMessageShown = false;

        } catch (Exception exception) {
            showConnectionFailure();
        }
    }

    public void refreshUnreadCount() {
        try {
            if (mainView == null) {
                return;
            }

            String token = mainView.getCurrentToken();
            String memberId = resolveMemberId(token);

            if (memberId != null) {
                updateBadge(memberId);
            }

            connectionMessageShown = false;

        } catch (Exception exception) {
            showConnectionFailure();
        }
    }

    private void showConnectionFailure() {
        if (view == null || connectionMessageShown) {
            return;
        }

        connectionMessageShown = true;
        view.showToastMessage(CONNECTION_ERROR_MESSAGE);
    }
}