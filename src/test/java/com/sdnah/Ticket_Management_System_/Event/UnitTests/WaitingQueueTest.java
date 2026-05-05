package com.sdnah.Ticket_Management_System_.Event.UnitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Waiting_Queue.WaitingQueue;

import java.util.List;
import static org.assertj.core.api.Assertions.*;

class WaitingQueueTest {

    private WaitingQueue waitingQueue;
    private final Long SHOW_ID = 1L;
    private final int CAPACITY_PER_MINUTE = 2; // Low capacity makes testing math easier

    @BeforeEach
    void setUp() {
        // Initialize with a capacity of 2 users per minute
        waitingQueue = new WaitingQueue(SHOW_ID, CAPACITY_PER_MINUTE);
    }

    @Test
    @DisplayName("Should add a user to the queue successfully")
    void testJoinQueueSuccess() {
        boolean joined = waitingQueue.joinQueue(101L);

        assertThat(joined).isTrue();
        assertThat(waitingQueue.getTotalWaiting()).isEqualTo(1);
        assertThat(waitingQueue.getPosition(101L)).isEqualTo(0); // Front of the line
    }

    @Test
    @DisplayName("Should prevent the same user from joining twice")
    void testJoinQueueDuplicate() {
        waitingQueue.joinQueue(101L);
        boolean joinedAgain = waitingQueue.joinQueue(101L);

        assertThat(joinedAgain).isFalse();
        assertThat(waitingQueue.getTotalWaiting()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should calculate correct position in line")
    void testGetPosition() {
        waitingQueue.joinQueue(101L); // Pos 0
        waitingQueue.joinQueue(102L); // Pos 1
        waitingQueue.joinQueue(103L); // Pos 2

        assertThat(waitingQueue.getPosition(103L)).isEqualTo(2);
        assertThat(waitingQueue.getPosition(999L)).isEqualTo(-1); // Not in line
    }

    @Test
    @DisplayName("Should calculate estimated wait time based on capacity")
    void testWaitTimeCalculation() {
        waitingQueue.joinQueue(101L); // Pos 0 -> 0 min
        waitingQueue.joinQueue(102L); // Pos 1 -> 0 min
        waitingQueue.joinQueue(103L); // Pos 2 -> 1 min (2/2)
        waitingQueue.joinQueue(104L); // Pos 3 -> 1 min (3/2)
        waitingQueue.joinQueue(105L); // Pos 4 -> 2 min (4/2)

        assertThat(waitingQueue.calculateEstimatedWaitTimeInMinutes(101L)).isEqualTo(0);
        assertThat(waitingQueue.calculateEstimatedWaitTimeInMinutes(103L)).isEqualTo(1);
        assertThat(waitingQueue.calculateEstimatedWaitTimeInMinutes(105L)).isEqualTo(2);
    }

    @Test
    @DisplayName("Should admit the correct amount of users and remove them from line")
    void testAdmitNextUsers() {
        waitingQueue.joinQueue(101L);
        waitingQueue.joinQueue(102L);
        waitingQueue.joinQueue(103L);

        List<Long> admitted = waitingQueue.admitNextUsers(2);

        assertThat(admitted).hasSize(2);
        assertThat(admitted).containsExactly(101L, 102L); // FIFO order
        assertThat(waitingQueue.getTotalWaiting()).isEqualTo(1);
        assertThat(waitingQueue.getPosition(103L)).isEqualTo(0); // 103 is now at the front
    }

    @Test
    @DisplayName("Should clear the queue entirely")
    void testClearQueue() {
        waitingQueue.joinQueue(101L);
        waitingQueue.joinQueue(102L);

        waitingQueue.clearQueue();

        assertThat(waitingQueue.getTotalWaiting()).isEqualTo(0);
    }
}
