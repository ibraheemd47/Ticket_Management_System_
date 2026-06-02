package com.sdnah.Ticket_Management_System_.Event.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Waiting_QueueService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Waiting_Queue.WaitingQueue;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.Waiting_QueueRepository;

/**
 * Coverage tests for the read-side and clearQueue branches that the existing
 * Waiting_QueueServiceTest doesn't reach.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Waiting_QueueService — Coverage Tests")
class Waiting_QueueServiceCoverageTest {

    @Mock private Waiting_QueueRepository repo;

    private Waiting_QueueService service;

    @BeforeEach
    void setUp() {
        service = new Waiting_QueueService(repo);
    }

    // ── getPosition ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPosition returns -1 when no queue exists for the show")
    void getPosition_noQueue() {
        when(repo.findById(42L)).thenReturn(Optional.empty());
        assertEquals(-1, service.getPosition(7L, 42L));
    }

    @Test
    @DisplayName("getPosition delegates to the WaitingQueue when present")
    void getPosition_withQueue() {
        WaitingQueue q = mock(WaitingQueue.class);
        when(q.getPosition(7L)).thenReturn(3);
        when(repo.findById(42L)).thenReturn(Optional.of(q));

        assertEquals(3, service.getPosition(7L, 42L));
    }

    // ── calculateEstimatedWaitTimeInMinutes ─────────────────────────────────

    @Test
    @DisplayName("calculateEstimatedWaitTimeInMinutes returns -1 when queue is missing")
    void eta_noQueue() {
        when(repo.findById(42L)).thenReturn(Optional.empty());
        assertEquals(-1, service.calculateEstimatedWaitTimeInMinutes(7L, 42L));
    }

    @Test
    @DisplayName("calculateEstimatedWaitTimeInMinutes delegates to the WaitingQueue when present")
    void eta_withQueue() {
        WaitingQueue q = mock(WaitingQueue.class);
        when(q.calculateEstimatedWaitTimeInMinutes(7L)).thenReturn(12);
        when(repo.findById(42L)).thenReturn(Optional.of(q));

        assertEquals(12, service.calculateEstimatedWaitTimeInMinutes(7L, 42L));
    }

    // ── clearQueue ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("clearQueue is a no-op when no queue exists for the show")
    void clearQueue_noQueue() {
        when(repo.findById(42L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.clearQueue(42L));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("clearQueue empties the queue and saves it when present")
    void clearQueue_withQueue() {
        WaitingQueue q = mock(WaitingQueue.class);
        when(repo.findById(42L)).thenReturn(Optional.of(q));

        service.clearQueue(42L);

        verify(q).clearQueue();
        verify(repo).save(q);
    }
}
