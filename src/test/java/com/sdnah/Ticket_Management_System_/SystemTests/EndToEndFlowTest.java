package com.sdnah.Ticket_Management_System_.SystemTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;


import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.TicketService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.ticket.TicketStatus;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * End-to-end system tests that string together the major user journeys:
 * register → verify → login → browse / select event → reserve ticket →
 * purchase ticket → scan ticket at door → logout. Exercises real Spring
 * services and the in-memory database — no mocks.
 *
 * The verification code path is exercised through the public service surface,
 * relying on {@code VerificationEmail.verifyCode} accepting any non-empty code
 * in test mode.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("System Tests — End-to-End User Journey")
class EndToEndFlowTest {

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IEventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    @Autowired
     private company_managment_serivce companyService;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void cleanDb() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        // JWTs are stateless — no token table to clean up.
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Walk a member through register → verify so they end up active and
     * verified. Returns the persisted memberId.
     */
    private UUID registerAndVerify(String username, String password, String email) {
        String memberId = userService.register(
                username, password, email, "0501234567", VerificationMethod.EMAIL);

        Member persisted = userRepository.findByUsername(username).orElseThrow();
        // VerificationEmail stores the code on the member; in test mode any non-empty
        // code passes verifyCode, so reusing the stored value also works.
        userService.verifyAccount(username, persisted.getVerificationCode());

        return UUID.fromString(memberId);
    }

    /**
     * Pre-seed an AVAILABLE ticket for a show, in its own transaction so the
     * Area is fully managed before the ticket is saved.
     */
    private UUID createAvailableTicket(UUID showId) {
        return txTemplate.execute(status -> {
            Area area = entityManager.merge(new Area("General Admission"));
            entityManager.flush();
            ticket t = new ticket(
                    UUID.randomUUID(),
                    showId,
                    area,
                    new Date(),
                    BigDecimal.valueOf(120));
            ticketRepository.save(t);
            return t.getTicketId();
        });
    }

    // -------------------------------------------------------------------------
    // The full happy-path journey
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Full user journey: register → verify → login → browse event → lock → purchase → scan → logout")
    void fullUserJourney_HappyPath_PersistsEachStep() {
        // -------- 1. Register the buyer and the event owner --------
        String buyerUsername = "buyer_" + UUID.randomUUID();
        String ownerUsername = "owner_" + UUID.randomUUID();

        UUID buyerId = registerAndVerify(buyerUsername, "secret123", buyerUsername + "@example.com");
        UUID ownerId = registerAndVerify(ownerUsername, "secret123", ownerUsername + "@example.com");

        assertNotNull(buyerId);
        assertNotNull(ownerId);
        assertTrue(userRepository.findByUsername(buyerUsername).orElseThrow().isVerified());
        assertTrue(userRepository.findByUsername(ownerUsername).orElseThrow().isVerified());

        // -------- 2. Both members log in --------
        String buyerToken = userService.login(buyerUsername, "secret123");
        String ownerToken = userService.login(ownerUsername, "secret123");

        assertNotNull(buyerToken);
        assertNotNull(ownerToken);
        assertTrue(userRepository.findByUsername(buyerUsername).orElseThrow().isLoggedin());
        assertTrue(userRepository.findByUsername(ownerUsername).orElseThrow().isLoggedin());

        // -------- 3. The owner creates a new event --------
       UUID companyId = companyService.openCompany(
        ownerToken,
        "Concert Company");

        Event event = eventService.createEvent(
                new EventDto(null, "End-to-End Concert", null, show_type.PERFORMANCE, "Tel Aviv"),
                companyId,
                999L); // companyId / eventOwnerId can be independent of the Member memberId

        assertNotNull(event.getEventId());

        // -------- 4. The buyer browses events and finds it --------
        List<Event> visibleEvents = eventService.searchEventsByName("End-to-End");
        assertEquals(1, visibleEvents.size());
        assertEquals(event.getEventId(), visibleEvents.get(0).getEventId());

        Event selected = eventService.getEventDetails(event.getEventId());
        assertEquals("End-to-End Concert", selected.getName());

        // -------- 5. A ticket is available for this event's show --------
        UUID showId = UUID.randomUUID();
        UUID ticketId = createAvailableTicket(showId);

        ticket beforeLock = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.AVAILABLE, beforeLock.getStatus());

        // -------- 6. Buyer locks the ticket in their cart --------
        assertTrue(ticketService.lockTicketForUser(ticketId, buyerId));

        ticket afterLock = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.LOCKED_IN_CART, afterLock.getStatus());
        assertEquals(buyerId, afterLock.getOwnerId());

        // -------- 7. Buyer confirms the purchase --------
        assertTrue(ticketService.confirmPurchase(ticketId, buyerId));

        ticket afterPurchase = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.PURCHASED, afterPurchase.getStatus());
        assertEquals(buyerId, afterPurchase.getOwnerId());

        // The buyer can now see the ticket in their owned-tickets list.
        List<ticket> myTickets = ticketService.getTicketsByOwner(buyerId);
        assertEquals(1, myTickets.size());
        assertEquals(ticketId, myTickets.get(0).getTicketId());

        // -------- 8. The buyer is scanned in at the door --------
        assertTrue(ticketService.scanTicketAtDoor(ticketId));
        assertEquals(TicketStatus.SCANNED,
                ticketRepository.findById(ticketId).orElseThrow().getStatus());

        // -------- 9. Logout --------
        userService.logout(buyerToken);
        assertFalse(userRepository.findByUsername(buyerUsername).orElseThrow().isLoggedin());
    }

    // -------------------------------------------------------------------------
    // Error / negative-path system flows
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Login is rejected before account verification has been completed")
    void loginRejected_WhenAccountNotYetVerified() {
        String username = "unverified_" + UUID.randomUUID();
        userService.register(username, "secret123", username + "@example.com",
                "0501234567", VerificationMethod.EMAIL);

        // No verifyAccount call — login must fail.
        assertThrows(RuntimeException.class,
                () -> userService.login(username, "secret123"));
    }

    @Test
    @DisplayName("Two buyers race for the same ticket — only the first lock wins")
    void twoBuyersRaceForOneTicket_OnlyFirstLockWins() {
        UUID buyerA = registerAndVerify("buyerA_" + UUID.randomUUID(), "secret123",
                "a_" + UUID.randomUUID() + "@example.com");
        UUID buyerB = registerAndVerify("buyerB_" + UUID.randomUUID(), "secret123",
                "b_" + UUID.randomUUID() + "@example.com");

        UUID ticketId = createAvailableTicket(UUID.randomUUID());

        // First buyer locks successfully.
        assertTrue(ticketService.lockTicketForUser(ticketId, buyerA));
        // Second buyer cannot lock the same ticket.
        assertFalse(ticketService.lockTicketForUser(ticketId, buyerB));

        ticket persisted = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.LOCKED_IN_CART, persisted.getStatus());
        assertEquals(buyerA, persisted.getOwnerId());
    }

    @Test
    @DisplayName("Releasing a locked ticket puts it back as AVAILABLE so another buyer can grab it")
    void releasingLockedTicket_MakesItAvailableAgain() {
        UUID buyerA = registerAndVerify("releaseA_" + UUID.randomUUID(), "secret123",
                "ra_" + UUID.randomUUID() + "@example.com");
        UUID buyerB = registerAndVerify("releaseB_" + UUID.randomUUID(), "secret123",
                "rb_" + UUID.randomUUID() + "@example.com");

        UUID ticketId = createAvailableTicket(UUID.randomUUID());

        assertTrue(ticketService.lockTicketForUser(ticketId, buyerA));

        ticketService.releaseTicket(ticketId);

        ticket released = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.AVAILABLE, released.getStatus());
        assertNull(released.getOwnerId());

        assertTrue(ticketService.lockTicketForUser(ticketId, buyerB));
        assertEquals(buyerB,
                ticketRepository.findById(ticketId).orElseThrow().getOwnerId());
    }

    @Test
    @DisplayName("Scanning a non-purchased ticket fails and the ticket status is unchanged")
    void scanningNonPurchasedTicket_Fails_AndStatusIsUnchanged() {
        UUID ticketId = createAvailableTicket(UUID.randomUUID());

        assertFalse(ticketService.scanTicketAtDoor(ticketId));

        assertEquals(TicketStatus.AVAILABLE,
                ticketRepository.findById(ticketId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Buyer who purchased multiple tickets sees them all in their owner-list")
    void buyerWithMultipleTickets_SeesAllOwnedTickets() {
        UUID buyerId = registerAndVerify("multi_" + UUID.randomUUID(), "secret123",
                "m_" + UUID.randomUUID() + "@example.com");

        UUID showId = UUID.randomUUID();
        UUID t1 = createAvailableTicket(showId);
        UUID t2 = createAvailableTicket(showId);
        UUID t3 = createAvailableTicket(showId);

        for (UUID id : List.of(t1, t2, t3)) {
            assertTrue(ticketService.lockTicketForUser(id, buyerId));
            assertTrue(ticketService.confirmPurchase(id, buyerId));
        }

        List<ticket> owned = ticketService.getTicketsByOwner(buyerId);
        assertEquals(3, owned.size());
        assertTrue(owned.stream().allMatch(t -> buyerId.equals(t.getOwnerId())));
        assertTrue(owned.stream().allMatch(t -> t.getStatus() == TicketStatus.PURCHASED));
    }

    @Test
    @DisplayName("Owner can edit the event after creation and the change is visible to a browsing buyer")
    void ownerEditsEvent_BuyerSeesUpdatedDetails() {
        String ownerUsername = "editor_" + UUID.randomUUID();
        registerAndVerify(ownerUsername, "secret123", ownerUsername + "@example.com");

        String ownerToken = userService.login(ownerUsername, "secret123");

        UUID companyId = companyService.openCompany(
                ownerToken,
                "Festival Company");

        Event event = eventService.createEvent(
                new EventDto(null, "Initial Title", null, show_type.FESTIVAL, "Haifa"),
                companyId,
                111L);

        eventService.editEventName(event.getEventId(), "Final Title", 111L);
        eventService.editEventVenue(event.getEventId(), "Heichal HaTarbut", 111L);

        Event seen = eventService.getEventDetails(event.getEventId());
        assertEquals("Final Title", seen.getName());
        assertEquals("Heichal HaTarbut", seen.getVenue());

        assertTrue(eventService.searchEventsByName("Final").stream()
                .anyMatch(e -> e.getEventId().equals(event.getEventId())));
    }
}
