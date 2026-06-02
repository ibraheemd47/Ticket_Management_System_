package com.sdnah.Ticket_Management_System_;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyRolesViewDTO;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CompanyAcceptanceTest {

    private CompanyRepository companyRepository;
    private Map<UUID, Company> companies;
    private company_managment_serivce companyService;

    private UserRepository userRepository;
    private IEventRepository eventRepository;
    private IrepresnteUserService representUserService;
    private NotificationService notificationService;

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
        notificationService = mock(NotificationService.class);

        companies = new HashMap<>();

        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            companies.put(company.getCompanyId(), company);
            return company;
        });

        when(companyRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID companyId = invocation.getArgument(0);
            return Optional.ofNullable(companies.get(companyId));
        });

        when(companyRepository.existsById(any(UUID.class))).thenAnswer(invocation -> {
            UUID companyId = invocation.getArgument(0);
            return companies.containsKey(companyId);
        });

        when(companyRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(companies.values()));

        doAnswer(invocation -> {
            UUID companyId = invocation.getArgument(0);
            companies.remove(companyId);
            return null;
        }).when(companyRepository).deleteById(any(UUID.class));

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
                representUserService,
                notificationService
        );

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

    @Test
    void openProductionCompanySuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "LiveNation");

        Company company = companyRepository.findById(companyId).orElseThrow();

        assertEquals(companyId, company.getCompanyId());
        assertEquals("LiveNation", company.getCompanyName());
        assertTrue(company.isOpen());
        assertEquals(FOUNDER_ID, company.getCompanyFounderId());
        assertTrue(company.getOwnerIds().contains(FOUNDER_ID));
    }

    @Test
    void openProductionCompanyWithMissingDetails() {
        assertThrows(RuntimeException.class,
                () -> companyService.openCompany(FOUNDER_TOKEN, ""));
    }

    @Test
    void openTwoCompaniesSuccessfully() {
        UUID companyA = companyService.openCompany(FOUNDER_TOKEN, "LiveNation");
        UUID companyB = companyService.openCompany(USER_200_TOKEN, "AnotherCompany");

        assertNotEquals(companyA, companyB);
        assertTrue(companyRepository.findById(companyA).isPresent());
        assertTrue(companyRepository.findById(companyB).isPresent());
    }

    @Test
    void viewActiveProductionCompaniesAndEventsSuccessfully() {
        UUID companyB = companyService.openCompany(USER_200_TOKEN, "CompanyB");
        UUID companyA = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        EventDto dto1 = new EventDto(null, "Event1", null, show_type.CONFERENCE, "Venue");
        EventDto dto2 = new EventDto(null, "Event2", null, show_type.CONFERENCE, "Venue");

        EventDto saved1 = companyService.addEvent(FOUNDER_TOKEN, companyA, dto1);
        EventDto saved2 = companyService.addEvent(FOUNDER_TOKEN, companyA, dto2);

        List<CompanyDTO> activeCompanies = companyService.getActiveCompanies();

        assertEquals(2, activeCompanies.size());

        CompanyDTO companyADto = activeCompanies.stream()
                .filter(c -> c.getCompanyId().equals(companyA))
                .findFirst()
                .orElseThrow();

        assertEquals("CompanyA", companyADto.getCompanyName());
        assertTrue(activeCompanies.stream().anyMatch(c -> c.getCompanyId().equals(companyB)));

        List<UUID> companyAEvents = companyService.getAllEventsByCompany(companyA);

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
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        List<CompanyDTO> activeCompanies = companyService.getActiveCompanies();

        assertEquals(1, activeCompanies.size());
        assertEquals(companyId, activeCompanies.get(0).getCompanyId());

        List<UUID> events = companyService.getAllEventsByCompany(companyId);
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void addEventSuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        EventDto dto = new EventDto(null, "Event", null, show_type.CONFERENCE, "Venue");
        EventDto saved = companyService.addEvent(FOUNDER_TOKEN, companyId, dto);

        Company company = companyRepository.findById(companyId).orElseThrow();
        assertTrue(company.getAssociatedEventIds().contains(saved.id));
    }

    @Test
    void removeEventSuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        EventDto dto = new EventDto(null, "Event", null, show_type.CONFERENCE, "Venue");
        EventDto saved = companyService.addEvent(FOUNDER_TOKEN, companyId, dto);

        Event event = new Event(dto.name, dto.eventType, companyId, FOUNDER_ID);
        try {
            var field = Event.class.getDeclaredField("eventId");
            field.setAccessible(true);
            field.set(event, saved.id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(eventRepository.findById(saved.id)).thenReturn(Optional.of(event));

        companyService.removeEvent(FOUNDER_TOKEN, companyId, saved.id);

        Company company = companyRepository.findById(companyId).orElseThrow();
        assertFalse(company.getAssociatedEventIds().contains(saved.id));
        verify(eventRepository).delete(event);
    }

    @Test
    void companyNotFoundWhenManagingEvents() {
        EventDto dto = new EventDto(null, "Event", null, show_type.CONFERENCE, "Venue");
        UUID missingCompanyId = UUID.randomUUID();

        assertThrows(RuntimeException.class,
                () -> companyService.addEvent(FOUNDER_TOKEN, missingCompanyId, dto));
    }

    @Test
    void userNotOwnerWhenManagingEvents() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        EventDto dto = new EventDto(null, "Event", null, show_type.CONFERENCE, "Venue");

        assertThrows(RuntimeException.class,
                () -> companyService.addEvent(USER_200_TOKEN, companyId, dto));
    }

    @Test
    void purchaseHistoryDisplayedSuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        Company company = companyRepository.findById(companyId).orElseThrow();
        company.addPurchaseRecord(11);
        company.addPurchaseRecord(12);
        company.addOrderRecord(21);
        companyRepository.save(company);

        List<Integer> purchaseHistory = companyService.getPurchaseHistory(FOUNDER_TOKEN, companyId);
        List<Integer> orderHistory = companyService.getOrderHistory(FOUNDER_TOKEN, companyId);

        assertEquals(2, purchaseHistory.size());
        assertEquals(1, orderHistory.size());
        assertTrue(purchaseHistory.contains(11));
        assertTrue(orderHistory.contains(21));
    }

    @Test
    void noHistoryFound() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        List<Integer> purchaseHistory = companyService.getPurchaseHistory(FOUNDER_TOKEN, companyId);
        List<Integer> orderHistory = companyService.getOrderHistory(FOUNDER_TOKEN, companyId);

        assertTrue(purchaseHistory.isEmpty());
        assertTrue(orderHistory.isEmpty());
    }

    @Test
    void userNotAuthorizedToViewHistory() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.getPurchaseHistory(USER_200_TOKEN, companyId));
    }

    @Test
    void managerAppointedSuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        companyService.appointManager(
                FOUNDER_TOKEN,
                companyId,
                USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS, CompanyPermission.VIEW_HISTORY));

        Company company = companyRepository.findById(companyId).orElseThrow();

        assertTrue(company.getManagers().contains(USER_200_ID));
        assertTrue(company.getManagerPermissionsView().get(USER_200_ID)
                .contains(CompanyPermission.MANAGE_EVENTS));
        assertTrue(company.getManagerPermissionsView().get(USER_200_ID)
                .contains(CompanyPermission.VIEW_HISTORY));
    }

    @Test
    void nomineeAlreadyManagerOrOwner() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, companyId, USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(RuntimeException.class,
                () -> companyService.appointManager(
                        FOUNDER_TOKEN, companyId, USER_200_ID,
                        Set.of(CompanyPermission.VIEW_HISTORY)));

        assertThrows(RuntimeException.class,
                () -> companyService.appointManager(
                        FOUNDER_TOKEN, companyId, FOUNDER_ID,
                        Set.of(CompanyPermission.MANAGE_EVENTS)));
    }

    @Test
    void userNotAuthorizedToAppointManager() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.appointManager(
                        USER_300_TOKEN, companyId, USER_200_ID,
                        Set.of(CompanyPermission.MANAGE_EVENTS)));
    }

    @Test
    void ownerAppointedSuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        companyService.appointAdditionalOwner(FOUNDER_TOKEN, companyId, USER_201_ID);

        Company company = companyRepository.findById(companyId).orElseThrow();
        assertTrue(company.getOwnerIds().contains(USER_201_ID));
    }

    @Test
    void nomineeAlreadyOwner() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, companyId, USER_201_ID);

        assertThrows(RuntimeException.class,
                () -> companyService.appointAdditionalOwner(FOUNDER_TOKEN, companyId, USER_201_ID));
    }

    @Test
    void ownerRemovedSuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, companyId, USER_201_ID);

        companyService.removeOwnerAppointment(FOUNDER_TOKEN, companyId, USER_201_ID);

        Company company = companyRepository.findById(companyId).orElseThrow();
        assertFalse(company.getOwnerIds().contains(USER_201_ID));
    }

    @Test
    void targetOwnerNotFound() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.removeOwnerAppointment(FOUNDER_TOKEN, companyId, USER_999_ID));
    }

    @Test
    void userNotAuthorizedToRemoveOwner() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, companyId, USER_201_ID);

        assertThrows(RuntimeException.class,
                () -> companyService.removeOwnerAppointment(USER_300_TOKEN, companyId, USER_201_ID));
    }

    @Test
    void ownershipResignedSuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, companyId, USER_201_ID);

        companyService.resignOwnership(USER_201_TOKEN, companyId);

        Company company = companyRepository.findById(companyId).orElseThrow();
        assertFalse(company.getOwnerIds().contains(USER_201_ID));
    }

    @Test
    void founderAttemptsResignation() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.resignOwnership(FOUNDER_TOKEN, companyId));
    }

    @Test
    void userNotOwnerResignationDenied() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.resignOwnership(USER_999_TOKEN, companyId));
    }

    @Test
    void successfulPermissionUpdate() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, companyId, USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS));

        companyService.modifyManagerPermissions(
                FOUNDER_TOKEN,
                companyId,
                USER_200_ID,
                Set.of(CompanyPermission.VIEW_HISTORY, CompanyPermission.RESPOND_TO_INQUIRIES));

        Company company = companyRepository.findById(companyId).orElseThrow();
        Set<CompanyPermission> updated = company.getManagerPermissionsView().get(USER_200_ID);

        assertTrue(updated.contains(CompanyPermission.VIEW_HISTORY));
        assertTrue(updated.contains(CompanyPermission.RESPOND_TO_INQUIRIES));
        assertFalse(updated.contains(CompanyPermission.MANAGE_EVENTS));
    }

    @Test
    void unauthorizedUpdate() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, companyId, USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(RuntimeException.class,
                () -> companyService.modifyManagerPermissions(
                        USER_300_TOKEN,
                        companyId,
                        USER_200_ID,
                        Set.of(CompanyPermission.VIEW_HISTORY)));
    }

    @Test
    void wrongManagerPermissionUpdate() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.modifyManagerPermissions(
                        FOUNDER_TOKEN,
                        companyId,
                        USER_999_ID,
                        Set.of(CompanyPermission.VIEW_HISTORY)));
    }

    @Test
    void successfulManagerRemoval() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, companyId, USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS));

        companyService.removeManagerAppointment(FOUNDER_TOKEN, companyId, USER_200_ID);

        Company company = companyRepository.findById(companyId).orElseThrow();
        assertFalse(company.getManagers().contains(USER_200_ID));
        assertFalse(company.getManagerPermissionsView().containsKey(USER_200_ID));
    }

    @Test
    void unauthorizedManagerRemoval() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointManager(FOUNDER_TOKEN, companyId, USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(RuntimeException.class,
                () -> companyService.removeManagerAppointment(USER_300_TOKEN, companyId, USER_200_ID));
    }

    @Test
    void wrongManagerRemoval() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.removeManagerAppointment(FOUNDER_TOKEN, companyId, USER_999_ID));
    }

    @Test
    void successfulClosure() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        boolean changed = companyService.closeCompany(FOUNDER_TOKEN, companyId);

        Company company = companyRepository.findById(companyId).orElseThrow();
        assertTrue(changed);
        assertFalse(company.isOpen());
    }

    @Test
    void unauthorizedClosure() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.closeCompany(USER_300_TOKEN, companyId));
    }

    @Test
    void alreadyClosed() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.closeCompany(FOUNDER_TOKEN, companyId);

        boolean changedAgain = companyService.closeCompany(FOUNDER_TOKEN, companyId);

        assertFalse(changedAgain);
    }

    @Test
    void successfulReopen() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.closeCompany(FOUNDER_TOKEN, companyId);

        boolean changed = companyService.reopenCompany(FOUNDER_TOKEN, companyId);

        Company company = companyRepository.findById(companyId).orElseThrow();
        assertTrue(changed);
        assertTrue(company.isOpen());
    }

    @Test
    void unauthorizedReopen() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.closeCompany(FOUNDER_TOKEN, companyId);

        assertThrows(RuntimeException.class,
                () -> companyService.reopenCompany(USER_300_TOKEN, companyId));
    }

    @Test
    void alreadyActive() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        boolean changed = companyService.reopenCompany(FOUNDER_TOKEN, companyId);

        assertFalse(changed);
    }

    @Test
    void viewRolesAndPermissionsSuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, companyId, USER_201_ID);
        companyService.appointManager(
                FOUNDER_TOKEN,
                companyId,
                USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS, CompanyPermission.VIEW_HISTORY));

        CompanyRolesViewDTO rolesView = companyService.viewRolesAndPermissions(FOUNDER_TOKEN, companyId);

        assertEquals(companyId, rolesView.getCompanyId());
        assertEquals(FOUNDER_ID, rolesView.getFounderId());
        assertTrue(rolesView.getOwnerIds().contains(FOUNDER_ID));
        assertTrue(rolesView.getOwnerIds().contains(USER_201_ID));
        assertTrue(rolesView.getManagerPermissions().containsKey(USER_200_ID));
        assertTrue(rolesView.getManagerPermissions().get(USER_200_ID)
                .contains(CompanyPermission.MANAGE_EVENTS));
    }

    @Test
    void emptyRolesDisplayed() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        CompanyRolesViewDTO rolesView = companyService.viewRolesAndPermissions(FOUNDER_TOKEN, companyId);

        assertNotNull(rolesView);
        assertTrue(rolesView.getManagerPermissions().isEmpty());
    }

    @Test
    void unauthorizedAccessToRolesView() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.viewRolesAndPermissions(USER_300_TOKEN, companyId));
    }

    @Test
    void crossCompanyAccessDenied() {
        companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        UUID companyB = companyService.openCompany(USER_200_TOKEN, "CompanyB");

        assertThrows(RuntimeException.class,
                () -> companyService.viewRolesAndPermissions(FOUNDER_TOKEN, companyB));
    }

    @Test
    void systemAdminClosesProductionCompanySuccessfully() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.appointAdditionalOwner(FOUNDER_TOKEN, companyId, USER_201_ID);
        companyService.appointManager(
                FOUNDER_TOKEN,
                companyId,
                USER_200_ID,
                Set.of(CompanyPermission.MANAGE_EVENTS));

        boolean changed = companyService.adminCloseCompany(ADMIN_TOKEN, companyId);

        Company company = companyRepository.findById(companyId).orElseThrow();

        assertTrue(changed);
        assertFalse(company.isOpen());
        assertTrue(company.getOwnerIds().isEmpty());
        assertTrue(company.getManagers().isEmpty());
        assertTrue(company.getManagerPermissionsView().isEmpty());
    }

    @Test
    void nonAdminCannotCloseProductionCompanyAsSystemAdmin() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");

        assertThrows(RuntimeException.class,
                () -> companyService.adminCloseCompany(USER_300_TOKEN, companyId));
    }

    @Test
    void systemAdminCloseAlreadyClosedCompanyReturnsFalse() {
        UUID companyId = companyService.openCompany(FOUNDER_TOKEN, "CompanyA");
        companyService.adminCloseCompany(ADMIN_TOKEN, companyId);

        boolean changedAgain = companyService.adminCloseCompany(ADMIN_TOKEN, companyId);

        assertFalse(changedAgain);
    }
}