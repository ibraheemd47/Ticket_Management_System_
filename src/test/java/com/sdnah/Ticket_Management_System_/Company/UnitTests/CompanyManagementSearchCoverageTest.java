package com.sdnah.Ticket_Management_System_.Company.UnitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

/**
 * Coverage tests for {@code company_managment_serivce} — focused on the
 * methods that {@code company_managment_serivceTest} doesn't reach:
 * the company-scoped search pipeline, {@code getEventDetails},
 * {@code getCompanyLogoURL}, {@code deleteCompany},
 * {@code searchCompaniesByKeyword}, {@code filterEventsByCompanyName},
 * {@code showCompaniesByRating}, {@code respondToInquiry},
 * {@code generateSalesReport}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("company_managment_serivce — Search + misc Coverage Tests")
class CompanyManagementSearchCoverageTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private IrepresnteUserService representUserService;
    @Mock private NotificationService notificationService;
    @Mock private PolicyRepository policyRepository;

    private company_managment_serivce service;

    private static final String TOKEN = "valid-token";
    private static final String MEMBER_ID = "m-001";

    // Shared event mocks (set up in @BeforeEach)
    private Event eventA;  // "Rock Night"   / venue "MSG"       / type PERFORMANCE / starts DAY_1, ends DAY_5 / rating 5
    private Event eventB;  // "Jazz Evening" / venue "Blue Note" / type PERFORMANCE / starts DAY_5, ends DAY_10 / rating 3
    private Event eventC;  // "Comedy Hour"  / venue "MSG"       / type FESTIVAL    / starts DAY_10, no endDate / rating 4

    private static final Date DAY_1  = new Date(0L);
    private static final Date DAY_5  = new Date(TimeUnit.DAYS.toMillis(5));
    private static final Date DAY_10 = new Date(TimeUnit.DAYS.toMillis(10));

    @BeforeEach
    void setUp() {
        service = new company_managment_serivce(
                companyRepository, userRepository, eventRepository,
                representUserService, notificationService, policyRepository);

        eventA = mockEvent("Rock Night",   "Loud and proud",     "MSG",
                show_type.PERFORMANCE, DAY_1,  DAY_5,  5);
        eventB = mockEvent("Jazz Evening", "Cool grooves",       "Blue Note",
                show_type.PERFORMANCE, DAY_5,  DAY_10, 3);
        eventC = mockEvent("Comedy Hour",  "Stand-up special",   "MSG",
                show_type.FESTIVAL,    DAY_10, null,   4);
    }

    private Event mockEvent(String name, String description, String venue,
                            show_type type, Date start, Date end, int rating) {
        Event e = mock(Event.class);
        lenient().when(e.getEventId()).thenReturn(UUID.randomUUID());
        lenient().when(e.getName()).thenReturn(name);
        lenient().when(e.getDescription()).thenReturn(description);
        lenient().when(e.getVenue()).thenReturn(venue);
        lenient().when(e.getEventType()).thenReturn(type);
        lenient().when(e.getStartDate()).thenReturn(start);
        lenient().when(e.getEndDate()).thenReturn(end);
        lenient().when(e.getReviews()).thenReturn(Map.of(UUID.randomUUID(), rating));
        return e;
    }

    /**
     * Wire up the eventsOfCompanyByName(...) chain so all three sample events
     * are reachable when a search is given the company "Acme".
     */
    private void stubAcmeWithSampleEvents() {
        Company acme = mock(Company.class);
        UUID eAId = eventA.getEventId();
        UUID eBId = eventB.getEventId();
        UUID eCId = eventC.getEventId();
        // lenient(): filterEventsByCompanyName doesn't hit eventRepository,
        // so those stubs may go unused — Mockito would complain by default.
        lenient().when(acme.getAssociatedEventIds()).thenReturn(List.of(eAId, eBId, eCId));
        lenient().when(companyRepository.findActiveByNameContaining(eq("Acme"), anyBoolean()))
                .thenReturn(List.of(acme));
        lenient().when(eventRepository.findById(eAId)).thenReturn(Optional.of(eventA));
        lenient().when(eventRepository.findById(eBId)).thenReturn(Optional.of(eventB));
        lenient().when(eventRepository.findById(eCId)).thenReturn(Optional.of(eventC));
    }

    // ── searchEventsInCompanyByDescription ──────────────────────────────────

    @Test
    @DisplayName("searchEventsInCompanyByDescription matches case-insensitive substring")
    void searchByDescription_matches() {
        stubAcmeWithSampleEvents();
        List<EventDto> hits = service.searchEventsInCompanyByDescription("Acme", "stand");
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).name).isEqualTo("Comedy Hour");
    }

    @Test
    @DisplayName("searchEventsInCompanyByDescription returns empty for blank input")
    void searchByDescription_blank() {
        assertThat(service.searchEventsInCompanyByDescription("Acme", null)).isEmpty();
        assertThat(service.searchEventsInCompanyByDescription("Acme", " ")).isEmpty();
    }

    // ── searchEventsInCompanyByKeyword ──────────────────────────────────────

    @Test
    @DisplayName("searchEventsInCompanyByKeyword matches either name or description")
    void searchByKeyword_nameOrDescription() {
        stubAcmeWithSampleEvents();
        // "cool" only appears in eventB description
        List<EventDto> hits = service.searchEventsInCompanyByKeyword("Acme", "cool");
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).name).isEqualTo("Jazz Evening");
        // "rock" matches eventA's name
        assertThat(service.searchEventsInCompanyByKeyword("Acme", "rock"))
                .singleElement().satisfies(d -> assertThat(d.name).isEqualTo("Rock Night"));
    }

    @Test
    @DisplayName("searchEventsInCompanyByKeyword returns empty for blank input")
    void searchByKeyword_blank() {
        assertThat(service.searchEventsInCompanyByKeyword("Acme", "")).isEmpty();
    }

    // ── searchEventsInCompanyByDateRange ────────────────────────────────────

    @Test
    @DisplayName("searchEventsInCompanyByDateRange filters by start date inside [from, to]")
    void searchByDateRange_filters() {
        stubAcmeWithSampleEvents();
        // [DAY_1, DAY_5] keeps eventA (DAY_1) + eventB (DAY_5)
        assertThat(service.searchEventsInCompanyByDateRange("Acme", DAY_1, DAY_5)).hasSize(2);
        // open-ended (both null) keeps every event with a startDate (all three)
        assertThat(service.searchEventsInCompanyByDateRange("Acme", null, null)).hasSize(3);
    }

    // ── searchEventsInCompanyByCategory / Start / End / Venue / MinRating ──

    @Test
    @DisplayName("searchEventsInCompanyByCategory matches event type, empty for null")
    void searchByCategory() {
        stubAcmeWithSampleEvents();
        assertThat(service.searchEventsInCompanyByCategory("Acme", show_type.PERFORMANCE))
                .hasSize(2);
        assertThat(service.searchEventsInCompanyByCategory("Acme", null)).isEmpty();
    }

    @Test
    @DisplayName("searchEventsInCompanyByStartDate keeps events starting on/after the date, empty for null")
    void searchByStartDate() {
        stubAcmeWithSampleEvents();
        assertThat(service.searchEventsInCompanyByStartDate("Acme", DAY_5)).hasSize(2);
        assertThat(service.searchEventsInCompanyByStartDate("Acme", null)).isEmpty();
    }

    @Test
    @DisplayName("searchEventsInCompanyByEndDate keeps events ending on/before, ignores events without endDate")
    void searchByEndDate() {
        stubAcmeWithSampleEvents();
        // eventA ends DAY_5 → in; eventB ends DAY_10 → out; eventC has no endDate → out
        List<EventDto> hits = service.searchEventsInCompanyByEndDate("Acme", DAY_5);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).name).isEqualTo("Rock Night");
        assertThat(service.searchEventsInCompanyByEndDate("Acme", null)).isEmpty();
    }

    @Test
    @DisplayName("searchEventsInCompanyByVenue matches substring case-insensitively, empty for blank")
    void searchByVenue() {
        stubAcmeWithSampleEvents();
        assertThat(service.searchEventsInCompanyByVenue("Acme", "msg")).hasSize(2);
        assertThat(service.searchEventsInCompanyByVenue("Acme", "")).isEmpty();
    }

    @Test
    @DisplayName("searchEventsInCompanyByMinRating filters by avg review score")
    void searchByMinRating() {
        stubAcmeWithSampleEvents();
        // eventA=5 in, eventC=4 in, eventB=3 out
        assertThat(service.searchEventsInCompanyByMinRating("Acme", 4.0)).hasSize(2);
        // No reviews → counts as 0; nothing fails the >=10 cutoff
        assertThat(service.searchEventsInCompanyByMinRating("Acme", 10.0)).isEmpty();
    }

    // ── getAllEventsByCompanyName / filterEventsByCompanyName ───────────────

    @Test
    @DisplayName("getAllEventsByCompanyName returns DTOs for every active company's events")
    void getAllEventsByCompanyName_returnsAll() {
        stubAcmeWithSampleEvents();
        assertThat(service.getAllEventsByCompanyName("Acme")).hasSize(3);
    }

    @Test
    @DisplayName("getAllEventsByCompanyName returns empty for blank input")
    void getAllEventsByCompanyName_blank() {
        assertThat(service.getAllEventsByCompanyName(null)).isEmpty();
        assertThat(service.getAllEventsByCompanyName("  ")).isEmpty();
    }

    @Test
    @DisplayName("filterEventsByCompanyName returns the eventId list (no DTOs)")
    void filterEventsByCompanyName_returnsIds() {
        stubAcmeWithSampleEvents();
        List<UUID> ids = service.filterEventsByCompanyName("Acme");
        assertThat(ids).hasSize(3);
        assertThat(service.filterEventsByCompanyName(null)).isEmpty();
    }

    // ── searchCompaniesByKeyword ────────────────────────────────────────────

    @Test
    @DisplayName("searchCompaniesByKeyword returns DTOs for OPEN companies that match the keyword")
    void searchCompaniesByKeyword_filtersClosed() {
        Company open = mock(Company.class);
        when(open.isOpen()).thenReturn(true);
        when(open.getCompanyName()).thenReturn("Acme Inc.");
        when(open.getCompanyId()).thenReturn(UUID.randomUUID());
        Company closed = mock(Company.class);
        when(closed.isOpen()).thenReturn(false);

        when(companyRepository.findByCompanyNameContainingIgnoreCase("acme"))
                .thenReturn(List.of(open, closed));

        List<CompanyDTO> hits = service.searchCompaniesByKeyword("acme");
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getCompanyName()).isEqualTo("Acme Inc.");
    }

    @Test
    @DisplayName("searchCompaniesByKeyword returns empty for blank input")
    void searchCompaniesByKeyword_blank() {
        assertThat(service.searchCompaniesByKeyword(null)).isEmpty();
        assertThat(service.searchCompaniesByKeyword("  ")).isEmpty();
    }

    // ── getEventDetails ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getEventDetails returns an EventDto for an existing event id")
    void getEventDetails_success() {
        UUID id = eventA.getEventId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(eventA));

        EventDto dto = service.getEventDetails(id);
        assertThat(dto).isNotNull();
        assertThat(dto.name).isEqualTo("Rock Night");
    }

    @Test
    @DisplayName("getEventDetails throws NoSuchElementException for an unknown id")
    void getEventDetails_notFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getEventDetails(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(id.toString());
    }

    // ── getCompanyLogoURL ───────────────────────────────────────────────────

    @Test
    @DisplayName("getCompanyLogoURL returns the company's logo URL")
    void getCompanyLogoURL_success() {
        UUID id = UUID.randomUUID();
        Company c = mock(Company.class);
        when(c.getLogoURL()).thenReturn("https://example.com/logo.png");
        when(companyRepository.findById(id)).thenReturn(Optional.of(c));

        assertThat(service.getCompanyLogoURL(id)).isEqualTo("https://example.com/logo.png");
    }

    @Test
    @DisplayName("getCompanyLogoURL throws when the company doesn't exist")
    void getCompanyLogoURL_notFound() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCompanyLogoURL(id))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── deleteCompany ───────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCompany removes the company when the actor is an owner")
    void deleteCompany_byOwner_success() {
        UUID id = UUID.randomUUID();
        Company c = mock(Company.class);
        when(c.isOwner(MEMBER_ID)).thenReturn(true);
        when(companyRepository.findById(id)).thenReturn(Optional.of(c));
        Member actor = new Member(MEMBER_ID, "alice", "h");
        when(representUserService.requireMember(TOKEN)).thenReturn(actor);

        service.deleteCompany(TOKEN, id);

        verify(companyRepository).deleteById(id);
    }

    @Test
    @DisplayName("deleteCompany refuses when the actor is not an owner")
    void deleteCompany_nonOwner_throws() {
        UUID id = UUID.randomUUID();
        Company c = mock(Company.class);
        when(c.isOwner(MEMBER_ID)).thenReturn(false);
        when(companyRepository.findById(id)).thenReturn(Optional.of(c));
        Member actor = new Member(MEMBER_ID, "alice", "h");
        when(representUserService.requireMember(TOKEN)).thenReturn(actor);

        assertThatThrownBy(() -> service.deleteCompany(TOKEN, id))
                .isInstanceOf(SecurityException.class);
        verify(companyRepository, never()).deleteById(any());
    }

    // ── showCompaniesByRating ───────────────────────────────────────────────

    @Test
    @DisplayName("showCompaniesByRating returns OPEN companies sorted by rating desc")
    void showCompaniesByRating_sortsDesc() {
        Company low  = mock(Company.class);
        Company high = mock(Company.class);
        when(low.getRating()).thenReturn(2.5);
        when(high.getRating()).thenReturn(4.8);
        when(low.getCompanyName()).thenReturn("Low");
        when(high.getCompanyName()).thenReturn("High");
        when(low.getCompanyId()).thenReturn(UUID.randomUUID());
        when(high.getCompanyId()).thenReturn(UUID.randomUUID());

        when(companyRepository.findByIsOpen(true)).thenReturn(List.of(low, high));

        List<CompanyDTO> result = service.showCompaniesByRating();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCompanyName()).isEqualTo("High");
        assertThat(result.get(1).getCompanyName()).isEqualTo("Low");
    }

    @Test
    @DisplayName("showCompaniesByRating0 is not implemented yet")
    void showCompaniesByRating0_unimplemented() {
        assertThatThrownBy(() -> service.showCompaniesByRating0())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ── respondToInquiry + generateSalesReport ──────────────────────────────

    @Test
    @DisplayName("respondToInquiry delegates to the Company domain and saves")
    void respondToInquiry_delegates() {
        UUID id = UUID.randomUUID();
        Company c = mock(Company.class);
        when(companyRepository.findById(id)).thenReturn(Optional.of(c));
        Member actor = new Member(MEMBER_ID, "alice", "h");
        when(representUserService.requireMember(TOKEN)).thenReturn(actor);

        service.respondToInquiry(TOKEN, id, 42, "Sorted out");

        verify(c).respondToInquiry(MEMBER_ID, 42, "Sorted out");
        verify(companyRepository).save(c);
    }

    @Test
    @DisplayName("generateSalesReport delegates to the Company domain")
    void generateSalesReport_delegates() {
        UUID id = UUID.randomUUID();
        Company c = mock(Company.class);
        when(companyRepository.findById(id)).thenReturn(Optional.of(c));
        Member actor = new Member(MEMBER_ID, "alice", "h");
        when(representUserService.requireMember(TOKEN)).thenReturn(actor);

        service.generateSalesReport(TOKEN, id);

        verify(c).generateSalesReport(MEMBER_ID);
    }
}
