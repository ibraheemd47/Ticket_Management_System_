package com.sdnah.Ticket_Management_System_.Event.UnitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sdnah.Ticket_Management_System_.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Waiting_QueueService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket.TicketStatus;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Waiting_Queue.WaitingQueue;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.Waiting_QueueRepository;

/**
 * Unit tests covering the buy-or-queue flow:
 *   1. Seats available  -> user gets the ticket directly, queue stays empty.
 *   2. No seats         -> user joins the waiting queue.
 *   3. Seat freed       -> queued user is admitted and finalizes the purchase.
 */
class Waiting_QueueServiceTest {

    @Mock private Waiting_QueueRepository waitingQueueRepository;
    @Mock private TicketRepository ticketRepository;

    private Waiting_QueueService waitingQueueService;
    private TicketService ticketService;

    private final long SHOW_ID = 42L;
    private final long USER_A = 101L;
    private final long USER_B = 202L;
    private final UUID USER_A_UUID = new UUID(0L, USER_A);
    private final UUID USER_B_UUID = new UUID(0L, USER_B);

    private UUID ticketId;
    private ticket sampleTicket;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        waitingQueueService = new Waiting_QueueService(waitingQueueRepository);
        ticketService = new TicketService(ticketRepository);

        ticketId = UUID.randomUUID();
        Area area = new Area("General Admission");
        sampleTicket = new ticket(ticketId, UUID.randomUUID(), area, new Date(), BigDecimal.TEN);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(sampleTicket));
    }

    @Test
    @DisplayName("Given an available ticket, when the user selects it, then the lock succeeds and the queue stays empty")
    void givenAvailableTicket_whenUserSelects_thenLockSucceedsAndQueueRemainsEmpty() {
        WaitingQueue queue = new WaitingQueue(SHOW_ID, 50);
        when(waitingQueueRepository.findById(SHOW_ID)).thenReturn(Optional.of(queue));

        boolean lockedDirectly = ticketService.lockTicketForUser(ticketId, USER_A_UUID);

        assertThat(lockedDirectly).as("user should grab the ticket without queueing").isTrue();
        assertThat(sampleTicket.getStatus()).isEqualTo(TicketStatus.LOCKED_IN_CART);
        assertThat(sampleTicket.getOwnerId()).isEqualTo(USER_A_UUID);
        assertThat(queue.getTotalWaiting()).isZero();
        assertThat(queue.getPosition(USER_A)).isEqualTo(-1);
        verify(waitingQueueRepository, never()).save(any());
        verify(ticketRepository, times(1)).save(sampleTicket);
    }

    @Test
    @DisplayName("Given no available seats, when the user tries to select a ticket, then they are placed in the waiting queue")
    void givenNoSeats_whenUserSelectsTicket_thenUserJoinsTheQueue() {
        sampleTicket.lockInCart(USER_B_UUID);
        boolean lockAttempt = ticketService.lockTicketForUser(ticketId, USER_A_UUID);
        assertThat(lockAttempt).as("locking a sold-out ticket must fail").isFalse();
        assertThat(sampleTicket.getOwnerId()).isEqualTo(USER_B_UUID);

        when(waitingQueueRepository.findById(SHOW_ID)).thenReturn(Optional.empty());

        boolean joined = waitingQueueService.joinQueue(USER_A, SHOW_ID);

        assertThat(joined).as("first time in line should succeed").isTrue();
        verify(waitingQueueRepository, times(1)).save(any(WaitingQueue.class));

        WaitingQueue persisted = new WaitingQueue(SHOW_ID, 50);
        persisted.joinQueue(USER_A);
        when(waitingQueueRepository.findById(SHOW_ID)).thenReturn(Optional.of(persisted));

        assertThat(waitingQueueService.getPosition(USER_A, SHOW_ID)).isEqualTo(0);
        assertThat(persisted.getTotalWaiting()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given a queued user, when a seat is freed, then they are admitted and can buy the ticket")
    void givenQueuedUser_whenSeatFreed_thenUserExitsQueueAndPurchases() {
        sampleTicket.lockInCart(USER_B_UUID);

        WaitingQueue queue = new WaitingQueue(SHOW_ID, 50);
        queue.joinQueue(USER_A);
        when(waitingQueueRepository.findById(SHOW_ID)).thenReturn(Optional.of(queue));

        ticketService.releaseTicket(ticketId);
        assertThat(sampleTicket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);

        List<Long> admitted = waitingQueueService.admitNextUsers(1, SHOW_ID);
        assertThat(admitted).containsExactly(USER_A);
        assertThat(queue.getTotalWaiting()).isZero();
        verify(waitingQueueRepository).save(queue);

        boolean nowLocked = ticketService.lockTicketForUser(ticketId, USER_A_UUID);
        assertThat(nowLocked).isTrue();
        assertThat(sampleTicket.getStatus()).isEqualTo(TicketStatus.LOCKED_IN_CART);

        boolean purchased = ticketService.confirmPurchase(ticketId, USER_A_UUID);
        assertThat(purchased).isTrue();
        assertThat(sampleTicket.getStatus()).isEqualTo(TicketStatus.PURCHASED);
    }

    @Test
    @DisplayName("Given a missing queue, when admitting users, then an empty list is returned and nothing is saved")
    void givenMissingQueue_whenAdmittingUsers_thenEmptyListIsReturned() {
        when(waitingQueueRepository.findById(eq(SHOW_ID))).thenReturn(Optional.empty());

        List<Long> admitted = waitingQueueService.admitNextUsers(5, SHOW_ID);

        assertThat(admitted).isEmpty();
        verify(waitingQueueRepository, never()).save(any());
    }
}
