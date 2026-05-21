package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Waiting_Queue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Transient;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;

@Entity
public class WaitingQueue {

    @Transient
    private Logger logger = (Logger) LoggerFactory.getLogger(WaitingQueue.class);

    @Id
    private Long showId;

    @ElementCollection
    @CollectionTable(name = "waiting_queue_entries", joinColumns = @JoinColumn(name = "show_id"))
    @OrderColumn(name = "position")
    private List<QueueEntry> line = new ArrayList<>();

    private int checkoutCapacityPerMinute;

    protected WaitingQueue() {
    }

    public WaitingQueue(Long showId, int checkoutCapacityPerMinute) {
        this.showId = showId;
        this.checkoutCapacityPerMinute = checkoutCapacityPerMinute;
        this.line = new ArrayList<>();
    }

    public boolean joinQueue(long userId) {
        QueueEntry newEntry = new QueueEntry(userId);
        if (line.contains(newEntry)) {
            logger.info("User {} is already in the queue.", userId);
            return false;
        }
        logger.info("User {} has joined the queue.", userId);
        return line.add(newEntry);
    }

    public int getPosition(long userId) {
        QueueEntry searchEntry = new QueueEntry(userId);
        return line.indexOf(searchEntry);
    }

    public int calculateEstimatedWaitTimeInMinutes(long userId) {
        int position = getPosition(userId);
        if (position == -1)
            return 0;
        return position / checkoutCapacityPerMinute;
    }

    public List<Long> admitNextUsers(int amountToAdmit) {
        List<Long> admittedUsers = new ArrayList<>();
        LinkedList<QueueEntry> deque = new LinkedList<>(line);
        for (int i = 0; i < amountToAdmit; i++) {
            if (deque.isEmpty())
                break;
            QueueEntry nextUser = deque.removeFirst();
            admittedUsers.add(nextUser.getUserId());
            logger.info("Admitted user {} from the queue.", nextUser.getUserId());
        }
        line.clear();
        line.addAll(deque);
        return admittedUsers;
    }

    public void clearQueue() {
        logger.info("Clearing the waiting queue for show {}", this.showId);
        line.clear();
    }

    public Long getShowId() {
        return showId;
    }

    public int getTotalWaiting() {
        return line.size();
    }

    // additional function

    public int getCheckoutCapacityPerMinute() {
        return checkoutCapacityPerMinute;
    }

    public void increaseFlow(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        checkoutCapacityPerMinute += amount;
    }

    public void decreaseFlow(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        checkoutCapacityPerMinute = Math.max(1, checkoutCapacityPerMinute - amount);
    }
}
