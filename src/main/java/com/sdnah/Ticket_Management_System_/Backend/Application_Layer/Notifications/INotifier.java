package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;

public interface INotifier {

    boolean notifyUser(String recipientUsername, NotificationDTO notification);
}