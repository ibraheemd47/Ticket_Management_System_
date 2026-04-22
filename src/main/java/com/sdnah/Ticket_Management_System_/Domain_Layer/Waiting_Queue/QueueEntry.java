package com.sdnah.Ticket_Management_System_.Domain_Layer.Waiting_Queue;

import java.time.LocalDateTime;

public class QueueEntry { // this class represents an entry in the waiting queue and also to measure the time span and how to deal with fairness 
    // the waiting queue will be implemented as a list of QueueEntry objects, each entry will have the userId and the time they joined the queue, this way we can measure the time span and also to deal with fairness by giving priority to those who have been waiting longer

    private final String userId;
    private final LocalDateTime joinTime;

    public QueueEntry(String userId) {
        this.userId = userId;
        this.joinTime = LocalDateTime.now();
    }

    public String getUserId() { return userId; }
    public LocalDateTime getJoinTime() { return joinTime; }
    
    // Equality based on userId so we don't put the same person in line twice
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueueEntry)) return false;
        QueueEntry that = (QueueEntry) o;
        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() { // this function is used to make sure that the same user doesn't get added to the queue twice, it will check if the userId is already in the queue and if it is it will return the same hash code so it will not be added again
        return userId.hashCode();
    }
}

