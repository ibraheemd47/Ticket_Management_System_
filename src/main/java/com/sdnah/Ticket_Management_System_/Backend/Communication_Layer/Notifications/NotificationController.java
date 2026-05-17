package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
private final IrepresnteUserService userService;
public NotificationController(NotificationService notificationService,
                              IrepresnteUserService userService) {
    this.notificationService = notificationService;
    this.userService = userService;
}

@GetMapping("/me")
public List<NotificationDTO> getMyNotifications(@RequestParam String token) {
    String memberId = userService.requireMemberId(token);
    return notificationService.getNotificationsForUser(memberId);
}

@GetMapping("/me/unread")
public List<NotificationDTO> getMyUnreadNotifications(@RequestParam String token) {
    String memberId = userService.requireMemberId(token);
    return notificationService.getUnreadNotificationsForUser(memberId);
}


@GetMapping("/me/unread/count")
public long getMyUnreadCount(@RequestParam String token) {
    String memberId = userService.requireMemberId(token);
    return notificationService.getUnreadCount(memberId);
}

@PatchMapping("/me/{notificationId}/read")
public void markMyNotificationAsRead(@RequestParam String token,
                                     @PathVariable String notificationId) {
    String memberId = userService.requireMemberId(token);
    notificationService.markAsRead(notificationId, memberId);
}

@PatchMapping("/me/read-all")
public void markAllMyNotificationsAsRead(@RequestParam String token) {
    String memberId = userService.requireMemberId(token);
    notificationService.markAllAsRead(memberId);
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