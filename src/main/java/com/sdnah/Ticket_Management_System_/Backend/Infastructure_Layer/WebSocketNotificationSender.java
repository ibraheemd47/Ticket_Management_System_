package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.RealtimeNotificationSender;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketNotificationSender implements RealtimeNotificationSender {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationSender(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendToUser(String recipientUsername, NotificationDTO notification) {
        messagingTemplate.convertAndSendToUser(
                recipientUsername,
                "/queue/notifications",
                notification
        );
    }
}