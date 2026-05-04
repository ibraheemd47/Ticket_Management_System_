package com.sdnah.Ticket_Management_System_.Event.IntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket.TicketStatus;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@Import(TicketService.class)
@EnableJpaRepositories(basePackages = "com.sdnah.Ticket_Management_System_.Infastructure_Layer", excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository.class))
class TicketServiceIntegrationTest {

    private static final String USER_ID = "user-1";
    private static final String OTHER_USER_ID = "user-2";

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID availableTicketId;
    private UUID lockedTicketId;
    private UUID purchasedTicketId;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();

        Area area = entityManager.merge(new Area("General Admission"));
        entityManager.flush();
        UUID showId = UUID.randomUUID();
        Date showDate = new Date();

        ticket available = new ticket(UUID.randomUUID(), showId, area, showDate, BigDecimal.TEN);
        ticket locked = new ticket(UUID.randomUUID(), showId, area, showDate, BigDecimal.TEN);
        locked.lockInCart(USER_ID);
        ticket purchased = new ticket(UUID.randomUUID(), showId, area, showDate, BigDecimal.TEN);
        purchased.lockInCart(USER_ID);
        purchased.purchase(USER_ID);

        ticketRepository.save(available);
        ticketRepository.save(locked);
        ticketRepository.save(purchased);

        availableTicketId = available.getTicketId();
        lockedTicketId = locked.getTicketId();
        purchasedTicketId = purchased.getTicketId();
    }

    @Test
    @DisplayName("Given available ticket, when locking for user, then status becomes LOCKED_IN_CART")
    void givenAvailableTicket_WhenLockingForUser_ThenStatusBecomesLockedInCart() {
        boolean result = ticketService.lockTicketForUser(availableTicketId, USER_ID);

        assertTrue(result);
        ticket saved = ticketRepository.findById(availableTicketId).orElseThrow();
        assertEquals(TicketStatus.LOCKED_IN_CART, saved.getStatus());
        assertEquals(USER_ID, saved.getOwnerId());
    }

    @Test
    @DisplayName("Given already locked ticket, when locking again, then false is returned")
    void givenAlreadyLockedTicket_WhenLockingAgain_ThenFalseIsReturned() {
        boolean result = ticketService.lockTicketForUser(lockedTicketId, OTHER_USER_ID);

        assertFalse(result);
        ticket saved = ticketRepository.findById(lockedTicketId).orElseThrow();
        assertEquals(USER_ID, saved.getOwnerId());
    }

    @Test
    @DisplayName("Given missing ticket id, when locking, then false is returned")
    void givenMissingTicketId_WhenLocking_ThenFalseIsReturned() {
        boolean result = ticketService.lockTicketForUser(UUID.randomUUID(), USER_ID);

        assertFalse(result);
    }

    @Test
    @DisplayName("Given locked ticket, when confirming purchase, then status becomes PURCHASED")
    void givenLockedTicket_WhenConfirmingPurchase_ThenStatusBecomesPurchased() {
        boolean result = ticketService.confirmPurchase(lockedTicketId, USER_ID);

        assertTrue(result);
        ticket saved = ticketRepository.findById(lockedTicketId).orElseThrow();
        assertEquals(TicketStatus.PURCHASED, saved.getStatus());
        assertEquals(USER_ID, saved.getOwnerId());
    }

    @Test
    @DisplayName("Given available ticket, when confirming purchase, then false is returned")
    void givenAvailableTicket_WhenConfirmingPurchase_ThenFalseIsReturned() {
        boolean result = ticketService.confirmPurchase(availableTicketId, USER_ID);

        assertFalse(result);
        ticket saved = ticketRepository.findById(availableTicketId).orElseThrow();
        assertEquals(TicketStatus.AVAILABLE, saved.getStatus());
    }

    @Test
    @DisplayName("Given locked ticket, when releasing, then ticket becomes AVAILABLE")
    void givenLockedTicket_WhenReleasing_ThenTicketBecomesAvailable() {
        ticketService.releaseTicket(lockedTicketId);

        ticket saved = ticketRepository.findById(lockedTicketId).orElseThrow();
        assertEquals(TicketStatus.AVAILABLE, saved.getStatus());
        assertNull(saved.getOwnerId());
    }

    @Test
    @DisplayName("Given purchased ticket, when scanning at door, then status becomes SCANNED")
    void givenPurchasedTicket_WhenScanningAtDoor_ThenStatusBecomesScanned() {
        boolean result = ticketService.scanTicketAtDoor(purchasedTicketId);

        assertTrue(result);
        ticket saved = ticketRepository.findById(purchasedTicketId).orElseThrow();
        assertEquals(TicketStatus.SCANNED, saved.getStatus());
    }

    @Test
    @DisplayName("Given non-purchased ticket, when scanning at door, then false is returned")
    void givenNonPurchasedTicket_WhenScanningAtDoor_ThenFalseIsReturned() {
        boolean result = ticketService.scanTicketAtDoor(availableTicketId);

        assertFalse(result);
    }

    @Test
    @DisplayName("Given owner with tickets, when fetching by owner, then only owned tickets are returned")
    void givenOwnerWithTickets_WhenFetchingByOwner_ThenOnlyOwnedTicketsAreReturned() {
        List<ticket> result = ticketService.getTicketsByOwner(USER_ID);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> USER_ID.equals(t.getOwnerId())));
    }

    @Test
    @DisplayName("Given owner with no tickets, when fetching by owner, then empty list is returned")
    void givenOwnerWithNoTickets_WhenFetchingByOwner_ThenEmptyListIsReturned() {
        List<ticket> result = ticketService.getTicketsByOwner("nobody");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Given full lifecycle, when running lock-purchase-scan, then state transitions persist")
    void givenFullLifecycle_WhenRunningLockPurchaseScan_ThenStateTransitionsPersist() {
        assertTrue(ticketService.lockTicketForUser(availableTicketId, USER_ID));
        assertTrue(ticketService.confirmPurchase(availableTicketId, USER_ID));
        assertTrue(ticketService.scanTicketAtDoor(availableTicketId));

        ticket saved = ticketRepository.findById(availableTicketId).orElseThrow();
        assertEquals(TicketStatus.SCANNED, saved.getStatus());
        assertEquals(USER_ID, saved.getOwnerId());
    }
}