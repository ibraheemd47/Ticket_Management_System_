package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications;

import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/notifications/debug")
public class NotificationDebugController {

    private final ConnectedUserRegistry connectedUserRegistry;

    public NotificationDebugController(ConnectedUserRegistry connectedUserRegistry) {
        this.connectedUserRegistry = connectedUserRegistry;
    }

    @GetMapping("/connected")
    public Set<String> getConnectedUsers() {
        return connectedUserRegistry.getConnectedUsers();
    }

    @GetMapping("/connected/{memberId}")
    public boolean isConnected(@PathVariable String memberId) {
        return connectedUserRegistry.isConnected(memberId);
    }

    @GetMapping("/connected/{memberId}/sessions")
    public int getSessionCount(@PathVariable String memberId) {
        return connectedUserRegistry.getSessionCount(memberId);
    }
}