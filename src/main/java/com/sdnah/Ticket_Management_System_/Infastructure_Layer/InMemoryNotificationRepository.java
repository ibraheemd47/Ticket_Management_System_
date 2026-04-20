package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.INotificationRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Notifications.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryNotificationRepository implements INotificationRepository {

    private final ConcurrentHashMap<String, Notification> notifications = new ConcurrentHashMap<>();

    @Override
    public void save(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null.");
        }
        notifications.put(notification.getId(), notification);
    }

    @Override
    public Optional<Notification> findById(String notificationId) {
        return Optional.ofNullable(notifications.get(notificationId));
    }

    @Override
    public List<Notification> findByRecipientUsername(String recipientUsername) {
        return notifications.values().stream()
                .filter(notification -> notification.belongsTo(recipientUsername))
                .toList();
    }

    @Override
    public List<Notification> findAll() {
        return new ArrayList<>(notifications.values());
    }

    @Override
    public void clear() {
        notifications.clear();
    }
}