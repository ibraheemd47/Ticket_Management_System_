package com.sdnah.Ticket_Management_System_;

import com.sdnah.Ticket_Management_System_.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.DTOs.CompanyDTO;
import com.sdnah.Ticket_Management_System_.DTOs.CompanyRolesViewDTO;
import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.UserRole;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CompanyAcceptanceTest {

    private CompanyRepository companyRepository;
    private Map<Integer, Company> companies;
    private company_managment_serivce companyService;

    private UserRepository userRepository;
    private IEventRepository eventRepository;
    private IrepresnteUserService representUserService;

    private final Map<String, Member> membersById = new HashMap<>();
    private final Map<String, Member> membersByToken = new HashMap<>();
    private final AtomicInteger eventSequence = new AtomicInteger(1);

    private static final String FOUNDER_ID = "100";
    private static final String USER_200_ID = "200";
    private static final String USER_300_ID = "300";
    private static final String USER_201_ID = "201";
    private static final String USER_999_ID = "999";

    private static final String ADMIN_ID = "500";
    private static final String ADMIN_TOKEN = "token500";

    private static final String FOUNDER_TOKEN = "token100";
    private static final String USER_200_TOKEN = "token200";
    private static final String USER_300_TOKEN = "token300";
    private static final String USER_201_TOKEN = "token201";
    private static final String USER_999_TOKEN = "token999";

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        userRepository = mock(UserRepository.class);
        eventRepository = mock(IEventRepository.class);
        representUserService = mock(IrepresnteUserService.class);

        companies = new HashMap<>();

        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            companies.put(company.getCompanyId(), company);
            return company;
        });

        when(companyRepository.findById(anyInt())).thenAnswer(invocation -> {
            int companyId = invocation.getArgument(0);
            return Optional.ofNullable(companies.get(companyId));
        });

        when(companyRepository.existsById(anyInt())).thenAnswer(invocation -> {
            int companyId = invocation.getArgument(0);
            return companies.containsKey(companyId);
        });

        when(companyRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(companies.values()));

        doAnswer(invocation -> {
            int companyId = invocation.getArgument(0);
            companies.remove(companyId);
            return null;
        }).when(companyRepository).deleteById(anyInt());

        when(userRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            membersById.put(member.getMemberId(), member);
            return member;
        });

        when(userRepository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.ofNullable(membersById.get(id));
        });

        when(userRepository.findByMemberId(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return membersById.get(id);
        });

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            if (event.getEventId() == null) {
                var field = Event.class.getDeclaredField("eventId");
                field.setAccessible(true);
                field.set(event, new UUID(0L, eventSequence.getAndIncrement()));
            }
            return event;
        });

        companyService = new company_managment_serivce(
                companyRepository,
                userRepository,
                eventRepository,
                representUserService);

        mockMember(FOUNDER_ID, "founder", FOUNDER_TOKEN);
        mockMember(USER_200_ID, "user200", USER_200_TOKEN);
        mockMember(USER_300_ID, "user300", USER_300_TOKEN);
        mockMember(USER_201_ID, "user201", USER_201_TOKEN);
        mockMember(USER_999_ID, "user999", USER_999_TOKEN);

        mockMember(ADMIN_ID, "admin", ADMIN_TOKEN);
        membersById.get(ADMIN_ID).setRole(UserRole.SYSTEM_ADMIN);
    }

    private void mockMember(String memberId, String username, String tokenValue) {
        Member member = new Member(memberId, username, "pass");
        member.setActive(true);
        member.setVerified(true);

        membersById.put(memberId, member);
        membersByToken.put(tokenValue, member);

        when(representUserService.requireMember(tokenValue)).thenReturn(member);
    }

    // II.3.2 Open Production Company
    @Test
    void openProductionCompanySuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "LiveNation");

        Company company = companyRepository.findById(1).orElseThrow();

        assertEquals(1, company.getCompanyId());
        assertEquals("LiveNation", company.getCompanyName());
        assertTrue(company.isOpen());
        assertEquals(FOUNDER_ID, company.getCompanyFounderId());
        assertTrue(company.getOwnerIds().contains(FOUNDER_ID));
    }

    @Test
    void openProductionCompanyWithMissingDetails() {
        assertThrows(RuntimeException.class,
                () -> companyService.openCompany(FOUNDER_TOKEN, 1, ""));
    }

    @Test
    void openProductionCompanyWithDuplicateIdentity() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "LiveNation");

        assertThrows(IllegalStateException.class,
                () -> companyService.openCompany(USER_200_TOKEN, 1, "AnotherCompany"));
    }

    // II.2.1 View Active Production Companies and Their Events
    @Test
    void viewActiveProductionCompaniesAndEventsSuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.openCompany(USER_200_TOKEN, 2, "CompanyB");

        EventDto dto1 = new EventDto(null, "Event1", null, show_type.CONFERENCE, "Venue");
        EventDto dto2 = new EventDto(null, "Event2", null, show_type.CONFERENCE, "Venue");

        EventDto saved1 = companyService.addEvent(FOUNDER_TOKEN, 1, dto1);
        EventDto saved2 = companyService.addEvent(FOUNDER_TOKEN, 1, dto2);

        List<CompanyDTO> activeCompanies = companyService.getActiveCompanies();

        assertEquals(2, activeCompanies.size());

        CompanyDTO companyA = activeCompanies.stream()
                .filter(c -> c.getCompanyId() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals("CompanyA", companyA.getCompanyName());

        List<UUID> companyAEvents = companyService.getAllEventsByCompany(1);

        assertEquals(2, companyAEvents.size());
        assertTrue(companyAEvents.contains(saved1.id));
        assertTrue(companyAEvents.contains(saved2.id));
    }

    @Test
    void noActiveProductionCompanies() {
        List<CompanyDTO> activeCompanies = companyService.getActiveCompanies();

        assertNotNull(activeCompanies);
        assertTrue(activeCompanies.isEmpty());
    }

    @Test
    void noEventsForActiveProductionCompany() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        List<CompanyDTO> activeCompanies = companyService.getActiveCompanies();

        assertEquals(1, activeCompanies.size());
        assertEquals(1, activeCompanies.get(0).getCompanyId());

        List<UUID> events = companyService.getAllEventsByCompany(1);
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    // II.4.1 Manage Events and Ticket Inventory
    @Test
    void addEventSuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        EventDto dto = new EventDto(null, "Event", null, show_type.CONFERENCE, "Venue");
        EventDto saved = companyService.addEvent(FOUNDER_TOKEN, 1, dto);

        Company company = companyRepository.findById(1).orElseThrow();
        assertTrue(company.getAssociatedEventIds().contains(saved.id));
    }

    @Test
    void removeEventSuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        EventDto dto = new EventDto(null, "Event", null, show_type.CONFERENCE, "Venue");
        EventDto saved = companyService.addEvent(FOUNDER_TOKEN, 1, dto);

        Event event = new Event(dto.name, dto.eventType, 1L, Long.valueOf(FOUNDER_ID));
        try {
            var field = Event.class.getDeclaredField("eventId");
            field.setAccessible(true);
            field.set(event, saved.id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(eventRepository.findById(saved.id)).thenReturn(Optional.of(event));

        companyService.removeEvent(FOUNDER_TOKEN, 1, saved.id);

        Company company = companyRepository.findById(1).orElseThrow();
        assertFalse(company.getAssociatedEventIds().contains(saved.id));
        verify(eventRepository).delete(event);
    }

    @Test
    void companyNotFoundWhenManagingEvents() {
        EventDto dto = new EventDto(null, "Event", null, show_type.CONFERENCE, "Venue");

        assertThrows(RuntimeException.class,
                () -> companyService.addEvent(FOUNDER_TOKEN, 999, dto));
    }

    @Test
    void userNotOwnerWhenManagingEvents() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        EventDto dto = new EventDto(null, "Event", null, show_type.CONFERENCE, "Venue");

        assertThrows(RuntimeException.class,
                () -> companyService.addEvent(USER_200_TOKEN, 1, dto));
    }

    // II.4.5 View Company Purchase and Order History
    @Test
    void purchaseHistoryDisplayedSuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        Company company = companyRepository.findById(1).orElseThrow();
        company.addPurchaseRecord(11);
        company.addPurchaseRecord(12);
        company.addOrderRecord(21);
        companyRepository.save(company);

        List<Integer> purchaseHistory = companyService.getPurchaseHistory(FOUNDER_TOKEN, 1);
        List<Integer> orderHistory = companyService.getOrderHistory(FOUNDER_TOKEN, 1);

        assertEquals(2, purchaseHistory.size());
        assertEquals(1, orderHistory.size());
        assertTrue(purchaseHistory.contains(11));
        assertTrue(orderHistory.contains(21));
    }

    @Test
    void noHistoryFound() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        List<Integer> purchaseHistory = companyService.getPurchaseHistory(FOUNDER_TOKEN, 1);
        List<Integer> orderHistory = companyService.getOrderHistory(FOUNDER_TOKEN, 1);

        assertTrue(purchaseHistory.isEmpty());
        assertTrue(orderHistory.isEmpty());
    }

    @Test
    void userNotAuthorizedToViewHistory() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.getPurchaseHistory(USER_200_TOKEN, 1));
    }

    // II.4.7 Appoint Company Manager
    @Test
    void managerAppointedSuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        companyService.appointManager(
                FOUNDER_TOKEN,
                1,
                USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS, CompanyPermission.VIEW_HISTORY));

        Company company = companyRepository.findById(1).orElseThrow();

        assertTrue(company.getManagers().contains(USER_200_ID));
        assertTrue(company.getManagerPermissionsView().get(USER_200_ID)
                .contains(CompanyPermission.MANAGE_EVENTS));
        assertTrue(company.getManagerPermissionsView().get(USER_200_ID)
                .contains(CompanyPermission.VIEW_HISTORY));
    }

    @Test
    void nomineeAlreadyManagerOrOwner() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, 1, USER_200_ID, Set.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(RuntimeException.class,
                () -> companyService.appointManager(
                        FOUNDER_TOKEN, 1, USER_200_ID, Set.of(CompanyPermission.VIEW_HISTORY)));

        assertThrows(RuntimeException.class,
                () -> companyService.appointManager(
                        FOUNDER_TOKEN, 1, FOUNDER_ID, Set.of(CompanyPermission.MANAGE_EVENTS)));
    }

    @Test
    void userNotAuthorizedToAppointManager() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.appointManager(
                        USER_300_TOKEN, 1, USER_200_ID, Set.of(CompanyPermission.MANAGE_EVENTS)));
    }

    // II.4.8 Appoint Additional Company Owner
    @Test
    void ownerAppointedSuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        companyService.appointAdditionalOwner(FOUNDER_TOKEN, 1, USER_201_ID);

        Company company = companyRepository.findById(1).orElseThrow();
        assertTrue(company.getOwnerIds().contains(USER_201_ID));
    }

    @Test
    void nomineeAlreadyOwner() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, 1, USER_201_ID);

        assertThrows(RuntimeException.class,
                () -> companyService.appointAdditionalOwner(FOUNDER_TOKEN, 1, USER_201_ID));
    }

    // II.4.9 Remove Company Owner Appointment
    @Test
    void ownerRemovedSuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, 1, USER_201_ID);

        companyService.removeOwnerAppointment(FOUNDER_TOKEN, 1, USER_201_ID);

        Company company = companyRepository.findById(1).orElseThrow();
        assertFalse(company.getOwnerIds().contains(USER_201_ID));
    }

    @Test
    void targetOwnerNotFound() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.removeOwnerAppointment(FOUNDER_TOKEN, 1, USER_999_ID));
    }

    @Test
    void userNotAuthorizedToRemoveOwner() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, 1, USER_201_ID);

        assertThrows(RuntimeException.class,
                () -> companyService.removeOwnerAppointment(USER_300_TOKEN, 1, USER_201_ID));
    }

    // II.4.10 Resign from Ownership
    @Test
    void ownershipResignedSuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, 1, USER_201_ID);

        companyService.resignOwnership(USER_201_TOKEN, 1);

        Company company = companyRepository.findById(1).orElseThrow();
        assertFalse(company.getOwnerIds().contains(USER_201_ID));
    }

    @Test
    void founderAttemptsResignation() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.resignOwnership(FOUNDER_TOKEN, 1));
    }

    @Test
    void userNotOwnerResignationDenied() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.resignOwnership(USER_999_TOKEN, 1));
    }

    // II.4.11 Modify Manager Permissions
    @Test
    void successfulPermissionUpdate() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, 1, USER_200_ID, Set.of(CompanyPermission.MANAGE_EVENTS));

        companyService.modifyManagerPermissions(
                FOUNDER_TOKEN,
                1,
                USER_200_ID,
                Set.of(CompanyPermission.VIEW_HISTORY, CompanyPermission.RESPOND_TO_INQUIRIES));

        Company company = companyRepository.findById(1).orElseThrow();
        Set<CompanyPermission> updated = company.getManagerPermissionsView().get(USER_200_ID);

        assertTrue(updated.contains(CompanyPermission.VIEW_HISTORY));
        assertTrue(updated.contains(CompanyPermission.RESPOND_TO_INQUIRIES));
        assertFalse(updated.contains(CompanyPermission.MANAGE_EVENTS));
    }

    @Test
    void unauthorizedUpdate() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, 1, USER_200_ID, Set.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(RuntimeException.class,
                () -> companyService.modifyManagerPermissions(
                        USER_300_TOKEN,
                        1,
                        USER_200_ID,
                        Set.of(CompanyPermission.VIEW_HISTORY)));
    }

    @Test
    void wrongManagerPermissionUpdate() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.modifyManagerPermissions(
                        FOUNDER_TOKEN,
                        1,
                        USER_999_ID,
                        Set.of(CompanyPermission.VIEW_HISTORY)));
    }

    // II.4.12 Remove Manager Appointment
    @Test
    void successfulManagerRemoval() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, 1, USER_200_ID, Set.of(CompanyPermission.MANAGE_EVENTS));

        companyService.removeManagerAppointment(FOUNDER_TOKEN, 1, USER_200_ID);

        Company company = companyRepository.findById(1).orElseThrow();
        assertFalse(company.getManagers().contains(USER_200_ID));
        assertFalse(company.getManagerPermissionsView().containsKey(USER_200_ID));
    }

    @Test
    void unauthorizedManagerRemoval() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, 1, USER_200_ID, Set.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(RuntimeException.class,
                () -> companyService.removeManagerAppointment(USER_300_TOKEN, 1, USER_200_ID));
    }

    @Test
    void wrongManagerRemoval() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.removeManagerAppointment(FOUNDER_TOKEN, 1, USER_999_ID));
    }

    // II.4.13 Suspend / Close Production Company
    @Test
    void successfulClosure() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        boolean changed = companyService.closeCompany(FOUNDER_TOKEN, 1);

        Company company = companyRepository.findById(1).orElseThrow();
        assertTrue(changed);
        assertFalse(company.isOpen());
    }

    @Test
    void unauthorizedClosure() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.closeCompany(USER_300_TOKEN, 1));
    }

    @Test
    void alreadyClosed() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.closeCompany(FOUNDER_TOKEN, 1);

        boolean changedAgain = companyService.closeCompany(FOUNDER_TOKEN, 1);

        assertFalse(changedAgain);
    }

    // II.4.14 Reopen Production Company
    @Test
    void successfulReopen() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.closeCompany(FOUNDER_TOKEN, 1);

        boolean changed = companyService.reopenCompany(FOUNDER_TOKEN, 1);

        Company company = companyRepository.findById(1).orElseThrow();
        assertTrue(changed);
        assertTrue(company.isOpen());
    }

    @Test
    void unauthorizedReopen() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.closeCompany(FOUNDER_TOKEN, 1);

        assertThrows(RuntimeException.class,
                () -> companyService.reopenCompany(USER_300_TOKEN, 1));
    }

    @Test
    void alreadyActive() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        boolean changed = companyService.reopenCompany(FOUNDER_TOKEN, 1);

        assertFalse(changed);
    }

    // II.4.15 View Roles and Permissions
    @Test
    void viewRolesAndPermissionsSuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, 1, USER_201_ID);
        companyService.appointManager(
                FOUNDER_TOKEN,
                1,
                USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS, CompanyPermission.VIEW_HISTORY));

        CompanyRolesViewDTO rolesView = companyService.viewRolesAndPermissions(FOUNDER_TOKEN, 1);

        assertEquals(1, rolesView.getCompanyId());
        assertEquals(FOUNDER_ID, rolesView.getFounderId());
        assertTrue(rolesView.getOwnerIds().contains(FOUNDER_ID));
        assertTrue(rolesView.getOwnerIds().contains(USER_201_ID));
        assertTrue(rolesView.getManagerPermissions().containsKey(USER_200_ID));
        assertTrue(rolesView.getManagerPermissions().get(USER_200_ID)   
                .contains(CompanyPermission.MANAGE_EVENTS));
    }

    @Test
    void emptyRolesDisplayed() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        CompanyRolesViewDTO rolesView = companyService.viewRolesAndPermissions(FOUNDER_TOKEN, 1);

        assertNotNull(rolesView);
        assertTrue(rolesView.getManagerPermissions().isEmpty());
    }

    @Test
    void unauthorizedAccessToRolesView() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.viewRolesAndPermissions(USER_300_TOKEN, 1));
    }

    @Test
    void crossCompanyAccessDenied() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.openCompany(USER_200_TOKEN, 2, "CompanyB");

        assertThrows(RuntimeException.class,
                () -> companyService.viewRolesAndPermissions(FOUNDER_TOKEN, 2));
    }

    // II.6.1 Close Production Company by System Admin
    @Test
    void systemAdminClosesProductionCompanySuccessfully() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, 1, USER_201_ID);
        companyService.appointManager(
                FOUNDER_TOKEN,
                1,
                USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS));

        boolean changed = companyService.adminCloseCompany(ADMIN_TOKEN, 1);

        Company company = companyRepository.findById(1).orElseThrow();

        assertTrue(changed);
        assertFalse(company.isOpen());
        assertTrue(company.getOwnerIds().isEmpty());
        assertTrue(company.getManagers().isEmpty());
        assertTrue(company.getManagerPermissionsView().isEmpty());
    }

    @Test
    void nonAdminCannotCloseProductionCompanyAsSystemAdmin() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.adminCloseCompany(USER_300_TOKEN, 1));
    }

    @Test
    void systemAdminCloseAlreadyClosedCompanyReturnsFalse() {
        companyService.openCompany(FOUNDER_TOKEN, 1, "CompanyA");
        companyService.adminCloseCompany(ADMIN_TOKEN, 1);

        boolean changedAgain = companyService.adminCloseCompany(ADMIN_TOKEN, 1);

        assertFalse(changedAgain);
    }
}