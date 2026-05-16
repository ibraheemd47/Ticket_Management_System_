package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{username}")
    public List<NotificationDTO> getNotifications(@PathVariable String username) {
        return notificationService.getNotificationsForUser(username);
    }

    @GetMapping("/{username}/unread")
    public List<NotificationDTO> getUnreadNotifications(@PathVariable String username) {
        return notificationService.getUnreadNotificationsForUser(username);
    }

    @PatchMapping("/{username}/{notificationId}/read")
    public void markNotificationAsRead(@PathVariable String username,
                                       @PathVariable String notificationId) {
        notificationService.markAsRead(notificationId, username);
    }

    @GetMapping("/{username}/unread/count")
    public long getUnreadCount(@PathVariable String username) {
        return notificationService.getUnreadCount(username);
    }

    @PatchMapping("/{username}/read-all")
    public void markAllAsRead(@PathVariable String username) {
        notificationService.markAllAsRead(username);
    }
    @PostMapping("/debug/send")
public String debugSendNotification(@RequestParam String recipientMemberId,
                                    @RequestParam String message) {
    return notificationService.createNotification(
            recipientMemberId,
            message,
            com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType.ROLE_CHANGED
    );
}
}