package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.Notifications;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.INotifier;
import com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications.Broadcaster;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;

import org.springframework.stereotype.Component;


@Component
public class WebSocketNotifier implements INotifier {

    private final Broadcaster broadcaster;

    public WebSocketNotifier(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public boolean notifyUser(String recipientMemberId, NotificationDTO notification) {
        return broadcaster.broadcast(recipientMemberId, notification);
    }
}