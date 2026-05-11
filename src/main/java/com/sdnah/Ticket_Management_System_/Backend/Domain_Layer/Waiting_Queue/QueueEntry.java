package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Waiting_Queue;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;

@Embeddable
public class QueueEntry {

    @Transient
    private Logger logger = (Logger) LoggerFactory.getLogger(QueueEntry.class);

    private long userId;

    @Column(name = "join_time")
    private LocalDateTime joinTime;

    protected QueueEntry() {}

    public QueueEntry(long userId) {
        this.userId = userId;
        this.joinTime = LocalDateTime.now();
    }

    public long getUserId() { return userId; }
    public LocalDateTime getJoinTime() { return joinTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueueEntry)) return false;
        QueueEntry that = (QueueEntry) o;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(userId);
    }
}
