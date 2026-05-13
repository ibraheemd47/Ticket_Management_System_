package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;

public interface RealtimeNotificationSender {

    void sendToUser(String recipientUsername, NotificationDTO notification);
}