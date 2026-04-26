package com.sdnah.Ticket_Management_System_.Domain_Layer.Waiting_Queue;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import java.util.ArrayList;

public class WaitingQueue {

    private Logger logger = (Logger) LoggerFactory.getLogger(WaitingQueue.class);
    
    private final Long showId;
    
    // Using a LinkedList to maintain strict First-In, First-Out (FIFO) order
    private final LinkedList<QueueEntry> line;
    
    // Business Rule: How many users can our checkout system handle per minute? // non functional requirement
    private final int checkoutCapacityPerMinute;

    public WaitingQueue(Long showId, int checkoutCapacityPerMinute) {
        this.showId = showId;
        this.checkoutCapacityPerMinute = checkoutCapacityPerMinute;
        this.line = new LinkedList<>();
    }

// ------------------------------------------------------DOMAIN LOGIC METHODS ---------------------------------------------------------------------------------

    public boolean joinQueue(long userId) {
        QueueEntry newEntry = new QueueEntry(userId);
        logger.info("Attempting to join queue for user {}", userId);
        if (line.contains(newEntry)) { // to prevent the same user from joining the queue multiple times, we check if they are already in line before adding them
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
        
        if (position == -1) {
            return 0; // Not in line
        }
        
        return position / checkoutCapacityPerMinute; // this will calculate the estimated wait time based on the user's position in the queue and the checkout capacity.
    }

    public List<Long> admitNextUsers(int amountToAdmit) { // this function will be the amount of users that we can admit (will have the opportunity to purchase tickets) to the checkout system per minute, it will take the first "amountToAdmit" users from the front of the line and return their userIds in a list, it will also remove them from the line
        List<Long> admittedUsers = new ArrayList<>();
        
        for (int i = 0; i < amountToAdmit; i++) {
            if (line.isEmpty()) {
                logger.info("No users to admit from the queue.");
                break;
            }
            // removeFirst() takes the person at the front of the line
            QueueEntry nextUser = line.removeFirst(); 
            admittedUsers.add(nextUser.getUserId());
            logger.info("Admitted user {} from the queue.", nextUser.getUserId());
        }
        
        return admittedUsers;
    }
    public void clearQueue() {
        logger.info("Clearing the waiting queue for show {}", this.showId);
        line.clear();
    }

    // Getters
    public Long getShowId() { return showId; }
    public int getTotalWaiting() { return line.size(); }
}
