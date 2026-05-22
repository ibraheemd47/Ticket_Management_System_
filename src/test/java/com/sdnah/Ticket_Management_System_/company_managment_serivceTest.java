package com.sdnah.Ticket_Management_System_;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.UserRole;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class company_managment_serivceTest {

    private company_managment_serivce service;

    private CompanyRepository repo;
    private UserRepository userRepository;
    private IEventRepository eventRepository;
    private IrepresnteUserService representUserService;
    private NotificationService notificationService;

    private UUID companyId;

    private static final String FOUNDER = "100";
    private static final String OWNER = "200";
    private static final String MANAGER = "300";
    private static final String USER = "999";

    private static final String ADMIN = "500";
    private static final String ADMIN_TOKEN = "token-admin";

    private static final String FOUNDER_TOKEN = "token-founder";
    private static final String OWNER_TOKEN = "token-owner";
    private static final String MANAGER_TOKEN = "token-manager";
    private static final String USER_TOKEN = "token-user";

    private final Map<String, Member> membersById = new HashMap<>();

    @BeforeEach
    void setUp() {
        repo = mock(CompanyRepository.class);
        userRepository = mock(UserRepository.class);
        eventRepository = mock(IEventRepository.class);
        representUserService = mock(IrepresnteUserService.class);
        notificationService = mock(NotificationService.class);

        service = new company_managment_serivce(
            repo,
            userRepository,
            eventRepository,
            representUserService,
            notificationService);

        Company company = new Company("Main Company", FOUNDER);
        companyId = company.getCompanyId();
        

        when(repo.findById(companyId)).thenReturn(Optional.of(company));
        when(repo.existsById(companyId)).thenReturn(true);
        when(repo.findAll()).thenReturn(List.of(company));
        when(repo.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMember(FOUNDER_TOKEN, FOUNDER);
        mockMember(OWNER_TOKEN, OWNER);
        mockMember(MANAGER_TOKEN, MANAGER);
        mockMember(USER_TOKEN, USER);

        mockMember(ADMIN_TOKEN, ADMIN);
        membersById.get(ADMIN).setRole(UserRole.SYSTEM_ADMIN);
    }

    @Test
    void GivenNewCompany_WhenOpenCompany_ThenCompanySaved() {
        UUID createdCompanyId = service.openCompany(FOUNDER_TOKEN, "New Company");

        assertNotNull(createdCompanyId);

        verify(repo).save(argThat(company ->
                company.getCompanyId() != null
                        && company.getCompanyName().equals("New Company")
                        && company.getCompanyFounderId().equals(FOUNDER)
        ));
    }

    // @Test
    // void GivenExistingCompanyId_WhenOpenCompany_ThenFail() {
    //     assertThrows(IllegalStateException.class,
    //             () -> service.openCompany(FOUNDER_TOKEN, "Duplicate"));
    // }

    @Test
    void GivenInvalidCompanyData_WhenOpenCompany_ThenFail() {
        assertAll(
                // () -> assertThrows(IllegalArgumentException.class,
                //         () -> service.openCompany(FOUNDER_TOKEN, "A")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.openCompany(FOUNDER_TOKEN, "")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.openCompany(FOUNDER_TOKEN, null)));
    }

    @Test
    void GivenOpenAndClosedCompanies_WhenGetActiveCompanies_ThenOnlyOpenReturned() {
        Company mainCompany = new Company( "Main Company", FOUNDER);
        Company closedCompany = new Company("Closed Company", FOUNDER);

        closedCompany.closeCompany(FOUNDER);

        when(repo.findAll()).thenReturn(List.of(mainCompany, closedCompany));

        List<CompanyDTO> active = service.getActiveCompanies();

        assertEquals(1, active.size());
        assertEquals(mainCompany.getCompanyId(), active.get(0).getCompanyId());
        assertEquals("Main Company", active.get(0).getCompanyName());
        assertTrue(active.get(0).isOpen());
    }

    @Test
    void GivenFounder_WhenAddEvent_ThenEventAdded() {
        EventDto dto = eventDto("Event10");
        Event event = new Event(dto.name, dto.eventType, companyId, Long.valueOf(FOUNDER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDto saved = service.addEvent(FOUNDER_TOKEN, companyId, dto);

        assertTrue(repo.findById(companyId).orElseThrow().getAssociatedEventIds().contains(saved.id));
    }

    @Test
    void GivenUnauthorizedUser_WhenAddEvent_ThenFail() {
        EventDto dto = eventDto("Event10");

        assertThrows(RuntimeException.class,
                () -> service.addEvent(USER_TOKEN, companyId, dto));
    }

    @Test
    void GivenMissingCompany_WhenAddEvent_ThenFail() {
        EventDto dto = eventDto("Event10");
        UUID missingCompanyId = UUID.randomUUID();

        when(repo.findById(missingCompanyId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.addEvent(FOUNDER_TOKEN, missingCompanyId, dto));
    }

    @Test
    void GivenDuplicateEvent_WhenAddEvent_ThenFail() {
        EventDto dto = eventDto("Event10");
        Event event = new Event(dto.name, dto.eventType, companyId, Long.valueOf(FOUNDER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        service.addEvent(FOUNDER_TOKEN, companyId, dto);

        assertThrows(IllegalArgumentException.class,
                () -> service.addEvent(FOUNDER_TOKEN, companyId, dto));
    }

    @Test
    void GivenExistingEvent_WhenRemoveEvent_ThenEventRemoved() {
        EventDto dto = eventDto("Event10");
        Event event = new Event(dto.name, dto.eventType, companyId, Long.valueOf(FOUNDER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDto saved = service.addEvent(FOUNDER_TOKEN, companyId, dto);

        when(eventRepository.findById(saved.id)).thenReturn(Optional.of(event));

        service.removeEvent(FOUNDER_TOKEN, companyId, saved.id);

        assertFalse(repo.findById(companyId).orElseThrow().getAssociatedEventIds().contains(saved.id));
        verify(eventRepository).delete(event);
    }

    @Test
    void GivenMissingEvent_WhenRemoveEvent_ThenFail() {
        UUID missingEventId = UUID.randomUUID();

        when(eventRepository.findById(missingEventId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.removeEvent(FOUNDER_TOKEN, companyId, missingEventId));
    }

    @Test
    void GivenUnauthorizedUser_WhenRemoveEvent_ThenFail() {
        EventDto dto = eventDto("Event10");
        Event event = new Event(dto.name, dto.eventType, companyId, Long.valueOf(FOUNDER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDto saved = service.addEvent(FOUNDER_TOKEN, companyId, dto);
        when(eventRepository.findById(saved.id)).thenReturn(Optional.of(event));

        assertThrows(RuntimeException.class,
                () -> service.removeEvent(USER_TOKEN, companyId, saved.id));
    }

    @Test
    void GivenFounder_WhenAppointManager_ThenManagerAdded() {
        service.appointManager(
                FOUNDER_TOKEN,
                companyId,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

        assertTrue(repo.findById(companyId).orElseThrow().isManager(MANAGER));
    }

    @Test
    void GivenNonFounder_WhenAppointManager_ThenFail() {
        assertThrows(RuntimeException.class,
                () -> service.appointManager(
                        USER_TOKEN,
                        companyId,
                        MANAGER,
                        EnumSet.of(CompanyPermission.MANAGE_EVENTS)));
    }

    @Test
    void GivenManager_WhenAddEventWithPermission_ThenSuccess() {
        service.appointManager(
                FOUNDER_TOKEN,
                companyId,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

        EventDto dto = eventDto("Event20");
        Event event = new Event(dto.name, dto.eventType, companyId, Long.valueOf(MANAGER));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDto saved = service.addEvent(MANAGER_TOKEN, companyId, dto);

        assertTrue(repo.findById(companyId).orElseThrow().getAssociatedEventIds().contains(saved.id));
    }

    @Test
    void GivenManagerWithoutPermission_WhenAddEvent_ThenFail() {
        service.appointManager(
                FOUNDER_TOKEN,
                companyId,
                MANAGER,
                EnumSet.of(CompanyPermission.VIEW_HISTORY));

        EventDto dto = eventDto("Event20");

        assertThrows(RuntimeException.class,
                () -> service.addEvent(MANAGER_TOKEN, companyId, dto));
    }

    @Test
    void GivenFounder_WhenAppointAdditionalOwner_ThenOwnerAdded() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, companyId, OWNER);

        assertTrue(repo.findById(companyId).orElseThrow().isOwner(OWNER));
    }

    @Test
    void GivenNonOwner_WhenAppointAdditionalOwner_ThenFail() {
        assertThrows(RuntimeException.class,
                () -> service.appointAdditionalOwner(USER_TOKEN, companyId, OWNER));
    }

    @Test
    void GivenExistingOwner_WhenAppointAdditionalOwner_ThenFail() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, companyId, OWNER);

        assertThrows(IllegalArgumentException.class,
                () -> service.appointAdditionalOwner(FOUNDER_TOKEN, companyId, OWNER));
    }

    @Test
    void GivenOwner_WhenRemoveOwnerAppointment_ThenOwnerRemoved() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, companyId, OWNER);

        service.removeOwnerAppointment(FOUNDER_TOKEN, companyId, OWNER);

        assertFalse(repo.findById(companyId).orElseThrow().isOwner(OWNER));
    }

    @Test
    void GivenFounderTarget_WhenRemoveOwnerAppointment_ThenFail() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, companyId, OWNER);

        assertThrows(RuntimeException.class,
                () -> service.removeOwnerAppointment(OWNER_TOKEN, companyId, FOUNDER));
    }

    @Test
    void GivenOwner_WhenResignOwnership_ThenOwnerRemoved() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, companyId, OWNER);

        service.resignOwnership(OWNER_TOKEN, companyId);

        assertFalse(repo.findById(companyId).orElseThrow().isOwner(OWNER));
    }

    @Test
    void GivenFounder_WhenResignOwnership_ThenFail() {
        assertThrows(RuntimeException.class,
                () -> service.resignOwnership(FOUNDER_TOKEN, companyId));
    }

    @Test
    void GivenOwner_WhenModifyManagerPermissions_ThenPermissionsChanged() {
        service.appointManager(
                FOUNDER_TOKEN,
                companyId,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

        service.modifyManagerPermissions(
                FOUNDER_TOKEN,
                companyId,
                MANAGER,
                EnumSet.of(CompanyPermission.VIEW_HISTORY));

        Company c = repo.findById(companyId).orElseThrow();
        assertFalse(c.managerHasPermission(MANAGER, CompanyPermission.MANAGE_EVENTS));
        assertTrue(c.managerHasPermission(MANAGER, CompanyPermission.VIEW_HISTORY));
    }

    @Test
    void GivenWrongOwner_WhenModifyManagerPermissions_ThenFail() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, companyId, OWNER);
        service.appointManager(
                FOUNDER_TOKEN,
                companyId,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(RuntimeException.class,
                () -> service.modifyManagerPermissions(
                        OWNER_TOKEN,
                        companyId,
                        MANAGER,
                        EnumSet.of(CompanyPermission.VIEW_HISTORY)));
    }

    @Test
    void GivenOwner_WhenRemoveManagerAppointment_ThenManagerRemoved() {
        service.appointManager(
                FOUNDER_TOKEN,
                companyId,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

        service.removeManagerAppointment(FOUNDER_TOKEN, companyId, MANAGER);

        assertFalse(repo.findById(companyId).orElseThrow().isManager(MANAGER));
    }

    @Test
    void GivenRemovedManager_WhenAddEvent_ThenFail() {
        service.appointManager(
                FOUNDER_TOKEN,
                companyId,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

        service.removeManagerAppointment(FOUNDER_TOKEN, companyId, MANAGER);

        EventDto dto = eventDto("Event30");

        assertThrows(RuntimeException.class,
                () -> service.addEvent(MANAGER_TOKEN, companyId, dto));
    }

    @Test
    void GivenFounder_WhenCloseCompany_ThenCompanyClosed() {
        boolean result = service.closeCompany(FOUNDER_TOKEN, companyId);

        assertTrue(result);
        assertFalse(repo.findById(companyId).orElseThrow().isOpen());
    }

    @Test
    void GivenNonFounder_WhenCloseCompany_ThenFail() {
        assertThrows(RuntimeException.class,
                () -> service.closeCompany(USER_TOKEN, companyId));
    }

    @Test
    void GivenAlreadyClosedCompany_WhenCloseCompany_ThenReturnFalse() {
        service.closeCompany(FOUNDER_TOKEN, companyId);

        boolean result = service.closeCompany(FOUNDER_TOKEN, companyId);

        assertFalse(result);
    }

    @Test
    void GivenClosedCompany_WhenReopenCompany_ThenCompanyOpen() {
        service.closeCompany(FOUNDER_TOKEN, companyId);

        boolean result = service.reopenCompany(FOUNDER_TOKEN, companyId);

        assertTrue(result);
        assertTrue(repo.findById(companyId).orElseThrow().isOpen());
    }

    @Test
    void GivenNonFounder_WhenReopenCompany_ThenFail() {
        service.closeCompany(FOUNDER_TOKEN, companyId);

        assertThrows(RuntimeException.class,
                () -> service.reopenCompany(USER_TOKEN, companyId));
    }

    @Test
    void GivenNonOwner_WhenViewRolesAndPermissions_ThenFail() {
        assertThrows(RuntimeException.class,
                () -> service.viewRolesAndPermissions(USER_TOKEN, companyId));
    }

    private void mockMember(String tokenValue, String memberId) {
        Member member = new Member(memberId, "user" + memberId, "hashedPassword");
        member.setVerified(true);
        member.setActive(true);

        membersById.put(memberId, member);

        when(representUserService.requireMember(tokenValue)).thenReturn(member);
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userRepository.findByMemberId(memberId)).thenReturn(member);
    }

    private EventDto eventDto(String name) {
        return new EventDto(null, name, null, show_type.CONFERENCE, "Venue");
    }

    @Test
    void GivenSystemAdmin_WhenAdminCloseCompany_ThenCompanyClosedAndRolesCleared() {
        service.appointAdditionalOwner(FOUNDER_TOKEN, companyId, OWNER);
        service.appointManager(
                FOUNDER_TOKEN,
                companyId,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

        boolean result = service.adminCloseCompany(ADMIN_TOKEN, companyId);

        Company company = repo.findById(companyId).orElseThrow();

        assertTrue(result);
        assertFalse(company.isOpen());
        assertTrue(company.getOwnerIds().isEmpty());
        assertTrue(company.getManagers().isEmpty());
        verify(repo, atLeastOnce()).save(company);
    }

    @Test
    void GivenNonAdmin_WhenAdminCloseCompany_ThenFail() {
        assertThrows(RuntimeException.class,
                () -> service.adminCloseCompany(USER_TOKEN, companyId));
    }

    @Test
    void GivenAlreadyClosedCompany_WhenAdminCloseCompany_ThenReturnFalse() {
        service.adminCloseCompany(ADMIN_TOKEN, companyId);

        boolean result = service.adminCloseCompany(ADMIN_TOKEN, companyId);

        assertFalse(result);
    }
}