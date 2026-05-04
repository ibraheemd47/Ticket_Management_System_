package com.sdnah.Ticket_Management_System_;


import com.sdnah.Ticket_Management_System_.Application_Layer.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.DTOs.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.IEventRepository;

class company_managment_serivceTest {

    private company_managment_serivce service;
    // private FakeRepo repo;

    private CompanyRepository repo;
    private UserRepository userRepository;
    private TokenRepository tokenRepository;
    private IEventRepository eventRepository;

    private static final int COMPANY_ID = 1;

    private static final String FOUNDER = "100";
    private static final String OWNER = "200";
    private static final String MANAGER = "300";
    private static final String USER = "999";

    private static final String FOUNDER_TOKEN = "token-founder";
    private static final String OWNER_TOKEN = "token-owner";
    private static final String MANAGER_TOKEN = "token-manager";
    private static final String USER_TOKEN = "token-user";

    @BeforeEach
    void setUp() {
        repo = mock(CompanyRepository.class);
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(TokenRepository.class);

        eventRepository = mock(IEventRepository.class);

        service = new company_managment_serivce(
                repo,
                userRepository,
                tokenRepository,
                eventRepository
        );
        Company company = new Company(COMPANY_ID, "Main Company", FOUNDER);

        when(repo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(repo.existsById(COMPANY_ID)).thenReturn(true);
        when(repo.findAll()).thenReturn(List.of(company));
        when(repo.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockToken(FOUNDER_TOKEN, FOUNDER);
        mockToken(OWNER_TOKEN, OWNER);
        mockToken(MANAGER_TOKEN, MANAGER);
        mockToken(USER_TOKEN, USER);
    }

    @Test
    void GivenNewCompany_WhenOpenCompany_ThenCompanySaved() {
        when(repo.existsById(2)).thenReturn(false);

        service.openCompany(FOUNDER_TOKEN, 2, "New Company");

        verify(repo).save(argThat(company ->
                company.getCompanyId() == 2 &&
                company.getCompanyName().equals("New Company") &&
                company.getCompanyFounderId().equals(FOUNDER)
        ));
    }

    @Test
    void GivenExistingCompanyId_WhenOpenCompany_ThenFail() {
        assertThrows(IllegalStateException.class,
                () -> service.openCompany(FOUNDER_TOKEN, COMPANY_ID, "Duplicate"));
    }

    @Test
    void GivenInvalidCompanyData_WhenOpenCompany_ThenFail() {
            assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.openCompany(FOUNDER_TOKEN, 0, "Bad")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.openCompany(FOUNDER_TOKEN, 2, "")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.openCompany(FOUNDER_TOKEN, 2, null))
        );
    }

   @Test
    void GivenOpenAndClosedCompanies_WhenGetActiveCompanies_ThenOnlyOpenReturned() {
        Company mainCompany = new Company(COMPANY_ID, "Main Company", FOUNDER);
        Company closedCompany = new Company(2, "Closed Company", FOUNDER);

        closedCompany.closeCompany(FOUNDER);

        when(repo.findAll()).thenReturn(List.of(mainCompany, closedCompany));

        List<CompanyDTO> active = service.getActiveCompanies();

        assertEquals(1, active.size());
        assertEquals(COMPANY_ID, active.get(0).getCompanyId());
        assertEquals("Main Company", active.get(0).getCompanyName());
        assertTrue(active.get(0).isOpen());
    }

    @Test
    void GivenFounder_WhenAddEvent_ThenEventAdded() {
        EventDto dto = eventDto("Event10");
        Event event = new Event(dto.name, dto.eventType, Long.valueOf(COMPANY_ID), Long.valueOf(FOUNDER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDto saved = service.addEvent(FOUNDER_TOKEN, COMPANY_ID, dto);

        assertTrue(repo.findById(COMPANY_ID).get().getAssociatedEventIds().contains(saved.id));
    }

    @Test
    void GivenUnauthorizedUser_WhenAddEvent_ThenFail() {
        EventDto dto = eventDto("Event10");

        assertThrows(RuntimeException.class,
                () -> service.addEvent(USER_TOKEN, COMPANY_ID, dto));
    }

    @Test
    void GivenMissingCompany_WhenAddEvent_ThenFail() {
        EventDto dto = eventDto("Event10");

        assertThrows(NoSuchElementException.class,
                () -> service.addEvent(FOUNDER_TOKEN, 999, dto));
    }

    @Test
    void GivenDuplicateEvent_WhenAddEvent_ThenFail() {
        EventDto dto = eventDto("Event10");
        Event event = new Event(dto.name, dto.eventType, Long.valueOf(COMPANY_ID), Long.valueOf(FOUNDER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        service.addEvent(FOUNDER_TOKEN, COMPANY_ID, dto);

        assertThrows(IllegalArgumentException.class,
                () -> service.addEvent(FOUNDER_TOKEN, COMPANY_ID, dto));
    }

    @Test
    void GivenExistingEvent_WhenRemoveEvent_ThenEventRemoved() {
        EventDto dto = eventDto("Event10");
        Event event = new Event(dto.name, dto.eventType, Long.valueOf(COMPANY_ID), Long.valueOf(FOUNDER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDto saved = service.addEvent(FOUNDER_TOKEN, COMPANY_ID, dto);

        when(eventRepository.findById(saved.id)).thenReturn(Optional.of(event));

        service.removeEvent(FOUNDER_TOKEN, COMPANY_ID, saved.id);

        assertFalse(repo.findById(COMPANY_ID).get().getAssociatedEventIds().contains(saved.id));
        verify(eventRepository).delete(event);
    }

    @Test
    void GivenMissingEvent_WhenRemoveEvent_ThenFail() {
        UUID missingEventId = UUID.randomUUID();

        when(eventRepository.findById(missingEventId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.removeEvent(FOUNDER_TOKEN, COMPANY_ID, missingEventId));
    }

    @Test
    void GivenUnauthorizedUser_WhenRemoveEvent_ThenFail() {
        EventDto dto = eventDto("Event10");
        Event event = new Event(dto.name, dto.eventType, Long.valueOf(COMPANY_ID), Long.valueOf(FOUNDER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDto saved = service.addEvent(FOUNDER_TOKEN, COMPANY_ID, dto);
        when(eventRepository.findById(saved.id)).thenReturn(Optional.of(event));

        assertThrows(RuntimeException.class,
                () -> service.removeEvent(USER_TOKEN, COMPANY_ID, saved.id));
    }

    @Test
    void GivenFounder_WhenAppointManager_ThenManagerAdded() {
        service.appointManager(
                FOUNDER_TOKEN,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        assertTrue(repo.findById(COMPANY_ID).get().isManager(MANAGER));
    }

    @Test
    void GivenNonFounder_WhenAppointManager_ThenFail() {
        assertThrows(RuntimeException.class,
                () -> service.appointManager(
                        USER_TOKEN,
                        COMPANY_ID,
                        MANAGER,
                        EnumSet.of(CompanyPermission.MANAGE_EVENTS)
                ));
    }

    @Test
    void GivenManager_WhenAddEventWithPermission_ThenSuccess() {
        service.appointManager(
                FOUNDER_TOKEN,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        EventDto dto = eventDto("Event20");
        Event event = new Event(dto.name, dto.eventType, Long.valueOf(COMPANY_ID), Long.valueOf(MANAGER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDto saved = service.addEvent(MANAGER_TOKEN, COMPANY_ID, dto);

        assertTrue(repo.findById(COMPANY_ID).get().getAssociatedEventIds().contains(saved.id));
    }

    @Test
    void GivenManagerWithoutPermission_WhenAddEvent_ThenFail() {
        service.appointManager(
                FOUNDER_TOKEN,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.VIEW_HISTORY)
        );

        EventDto dto = eventDto("Event20");

        assertThrows(RuntimeException.class,
                () -> service.addEvent(MANAGER_TOKEN, COMPANY_ID, dto));
    }

    @Test
    void GivenFounder_WhenAppointAdditionalOwner_ThenOwnerAdded() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, COMPANY_ID, OWNER);

        assertTrue(repo.findById(COMPANY_ID).get().isOwner(OWNER));
    }

    @Test
    void GivenNonOwner_WhenAppointAdditionalOwner_ThenFail() {
        assertThrows(SecurityException.class,
                () -> service.appointAdditionalOwner(USER, COMPANY_ID, OWNER));
    }

    @Test
    void GivenExistingOwner_WhenAppointAdditionalOwner_ThenFail() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, COMPANY_ID, OWNER);

        assertThrows(IllegalArgumentException.class,
                () -> service.appointAdditionalOwner(FOUNDER_TOKEN, COMPANY_ID, OWNER));
    }

    @Test
    void GivenOwner_WhenRemoveOwnerAppointment_ThenOwnerRemoved() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, COMPANY_ID, OWNER);

        service.removeOwnerAppointment(FOUNDER_TOKEN, COMPANY_ID, OWNER);

        assertFalse(repo.findById(COMPANY_ID).get().isOwner(OWNER));
    }

    @Test
    void GivenFounderTarget_WhenRemoveOwnerAppointment_ThenFail() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, COMPANY_ID, OWNER);

        assertThrows(RuntimeException.class,
                () -> service.removeOwnerAppointment(OWNER_TOKEN, COMPANY_ID, FOUNDER));
    }

    @Test
    void GivenOwner_WhenResignOwnership_ThenOwnerRemoved() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, COMPANY_ID, OWNER);

        service.resignOwnership(OWNER_TOKEN, COMPANY_ID);

        assertFalse(repo.findById(COMPANY_ID).get().isOwner(OWNER));
    }

    @Test
    void GivenFounder_WhenResignOwnership_ThenFail() {
        assertThrows(IllegalArgumentException.class,
                () -> service.resignOwnership(FOUNDER_TOKEN, COMPANY_ID));
    }

    @Test
    void GivenOwner_WhenModifyManagerPermissions_ThenPermissionsChanged() {
        service.appointManager(
                FOUNDER_TOKEN,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        service.modifyManagerPermissions(
                FOUNDER_TOKEN,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.VIEW_HISTORY)
        );

        Company c = repo.findById(COMPANY_ID).get();
        assertFalse(c.managerHasPermission(MANAGER, CompanyPermission.MANAGE_EVENTS));
        assertTrue(c.managerHasPermission(MANAGER, CompanyPermission.VIEW_HISTORY));
    }

    @Test
    void GivenWrongOwner_WhenModifyManagerPermissions_ThenFail() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, COMPANY_ID, OWNER);
        service.appointManager(
                FOUNDER_TOKEN,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        assertThrows(SecurityException.class,
                () -> service.modifyManagerPermissions(
                        OWNER_TOKEN,
                        COMPANY_ID,
                        MANAGER,
                        EnumSet.of(CompanyPermission.VIEW_HISTORY)
                ));
    }

    @Test
    void GivenOwner_WhenRemoveManagerAppointment_ThenManagerRemoved() {
        service.appointManager(
                FOUNDER_TOKEN,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        service.removeManagerAppointment(FOUNDER_TOKEN, COMPANY_ID, MANAGER);

        assertFalse(repo.findById(COMPANY_ID).get().isManager(MANAGER));
    }

    @Test
    void GivenRemovedManager_WhenAddEvent_ThenFail() {
        service.appointManager(
                FOUNDER_TOKEN,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        service.removeManagerAppointment(FOUNDER_TOKEN, COMPANY_ID, MANAGER);

        EventDto dto = eventDto("Event30");

        assertThrows(RuntimeException.class,
                () -> service.addEvent(MANAGER_TOKEN, COMPANY_ID, dto));
    }

    @Test
    void GivenFounder_WhenCloseCompany_ThenCompanyClosed() {
        boolean result = service.closeCompany(FOUNDER_TOKEN, COMPANY_ID);

        assertTrue(result);
        assertFalse(repo.findById(COMPANY_ID).get().isOpen());
    }

    @Test
    void GivenNonFounder_WhenCloseCompany_ThenFail() {
        assertThrows(SecurityException.class,
                () -> service.closeCompany(USER, COMPANY_ID));
    }

    @Test
    void GivenAlreadyClosedCompany_WhenCloseCompany_ThenReturnFalse() {
        service.closeCompany(FOUNDER_TOKEN, COMPANY_ID);

        boolean result = service.closeCompany(FOUNDER_TOKEN, COMPANY_ID);

        assertFalse(result);
    }

    @Test
    void GivenClosedCompany_WhenReopenCompany_ThenCompanyOpen() {
        service.closeCompany(FOUNDER_TOKEN, COMPANY_ID);

        boolean result = service.reopenCompany(FOUNDER_TOKEN, COMPANY_ID);

        assertTrue(result);
        assertTrue(repo.findById(COMPANY_ID).get().isOpen());
    }

    @Test
    void GivenNonFounder_WhenReopenCompany_ThenFail() {
        service.closeCompany(FOUNDER_TOKEN, COMPANY_ID);

        assertThrows(SecurityException.class,
                () -> service.reopenCompany(USER, COMPANY_ID));
    }

    

    @Test
    void GivenNonOwner_WhenViewRolesAndPermissions_ThenFail() {
        assertThrows(SecurityException.class,
                () -> service.viewRolesAndPermissions(USER, COMPANY_ID));
    }


    //helper func
    private void mockToken(String tokenValue, String memberId) {
        AuthToken token = new AuthToken(
                tokenValue,
                memberId,
                LocalDateTime.now().plusHours(1)
        );

        Member member = new Member(memberId, "user" + memberId, "hashedPassword");
        member.setVerified(true);
        member.setActive(true);

        when(tokenRepository.findById(tokenValue)).thenReturn(Optional.of(token));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userRepository.findByMemberId(memberId)).thenReturn(member);
    }

    private EventDto eventDto(String name) {
        return new EventDto(null, name, null, show_type.CONFERENCE, "Venue");
    }
}