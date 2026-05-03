package com.sdnah.Ticket_Management_System_;

import com.sdnah.Ticket_Management_System_.Application_Layer.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.CompanyRolesViewDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.MemoryCompanyRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CompanyAcceptanceTest {

    private MemoryCompanyRepository companyRepository;
    private company_managment_serivce companyService;

    private UserRepository userRepository;
    private TokenRepository tokenRepository;

    private String founderToken;
    private String user200Token;
    private String user300Token;

    @BeforeEach
    void setUp() {
        companyRepository = new MemoryCompanyRepository();
        userRepository = new UserRepository();
        tokenRepository = new TokenRepository();

        companyService = new company_managment_serivce(
                companyRepository,
                userRepository,
                tokenRepository
        );

        // יצירת משתמשים
        Member founder = new Member("100", "founder", "pass");
        founder.setActive(true);
        founder.setVerified(true);

        Member user200 = new Member("200", "user200", "pass");
        user200.setActive(true);
        user200.setVerified(true);

        Member user300 = new Member("300", "user300", "pass");
        user300.setActive(true);
        user300.setVerified(true);

        userRepository.save(founder);
        userRepository.save(user200);
        userRepository.save(user300);

        // יצירת tokens
        founderToken = "token100";
        user200Token = "token200";
        user300Token = "token300";

        tokenRepository.save(new AuthToken(founderToken, "100", LocalDateTime.now().plusDays(1)));
        tokenRepository.save(new AuthToken(user200Token, "200", LocalDateTime.now().plusDays(1)));
        tokenRepository.save(new AuthToken(user300Token, "300", LocalDateTime.now().plusDays(1)));
    }

   
    // II.3.2 Open Production Company
    @Test
    void openProductionCompanySuccessfully() {
        companyService.openCompany(1, "LiveNation", 100);

        Company company = companyRepository.findById(1).orElseThrow();

        assertEquals(1, company.getCompanyId());
        assertEquals("LiveNation", company.getCompanyName());
        assertTrue(company.isOpen());
        assertEquals(100, company.getCompanyFounderId());
        assertTrue(company.getOwnerIds().contains(100));
    }

    @Test
    void openProductionCompanyWithMissingDetails() {
        assertThrows(IllegalArgumentException.class,
                () -> companyService.openCompany(1, "", 100));
    }

    @Test
    void openProductionCompanyWithDuplicateIdentity() {
        companyService.openCompany(1, "LiveNation", 100);

        assertThrows(IllegalStateException.class,
                () -> companyService.openCompany(1, "AnotherCompany", 200));
    }

    // // II.2.1 View Active Production Companies and Their Events
    // @Test
    // void viewActiveProductionCompaniesAndEventsSuccessfully() {
    //     companyService.openCompany(1, "CompanyA", 100);
    //     companyService.openCompany(2, "CompanyB", 200);

    //     companyService.addEvent(100, 1, 1000);
    //     companyService.addEvent(100, 1, 1001);

    //     List<Company> activeCompanies = companyService.getActiveCompanies();

    //     assertEquals(2, activeCompanies.size());

    //     Company companyA = activeCompanies.stream()
    //             .filter(c -> c.getCompanyId() == 1)
    //             .findFirst()
    //             .orElseThrow();

    //     assertEquals(2, companyA.getAssociatedEventIds().size());
    //     assertTrue(companyA.getAssociatedEventIds().contains(1000));
    //     assertTrue(companyA.getAssociatedEventIds().contains(1001));
    // }

    // @Test
    // void noActiveProductionCompanies() {
    //     List<Company> activeCompanies = companyService.getActiveCompanies();

    //     assertNotNull(activeCompanies);
    //     assertTrue(activeCompanies.isEmpty());
    // }

    // @Test
    // void noEventsForActiveProductionCompany() {
    //     companyService.openCompany(1, "CompanyA", 100);

    //     List<Company> activeCompanies = companyService.getActiveCompanies();

    //     assertEquals(1, activeCompanies.size());
    //     assertTrue(activeCompanies.get(0).getAssociatedEventIds().isEmpty());
    // }

    // II.2.1 View Active Production Companies and Their Events
@Test
void viewActiveProductionCompaniesAndEventsSuccessfully() {
    companyService.openCompany(1, "CompanyA", 100);
    companyService.openCompany(2, "CompanyB", 200);

    companyService.addEvent(100, 1, 1000);
    companyService.addEvent(100, 1, 1001);

    List<CompanyDTO> activeCompanies = companyService.getActiveCompanies();

    assertEquals(2, activeCompanies.size());

    CompanyDTO companyA = activeCompanies.stream()
            .filter(c -> c.getCompanyId() == 1)
            .findFirst()
            .orElseThrow();

    assertEquals("CompanyA", companyA.getCompanyName());

    List<Integer> companyAEvents = companyService.getAllEventsByCompany(1);

    assertEquals(2, companyAEvents.size());
    assertTrue(companyAEvents.contains(1000));
    assertTrue(companyAEvents.contains(1001));
}

@Test
void noActiveProductionCompanies() {
    List<CompanyDTO> activeCompanies = companyService.getActiveCompanies();

    assertNotNull(activeCompanies);
    assertTrue(activeCompanies.isEmpty());
}

@Test
void noEventsForActiveProductionCompany() {
    companyService.openCompany(1, "CompanyA", 100);

    List<CompanyDTO> activeCompanies = companyService.getActiveCompanies();

    assertEquals(1, activeCompanies.size());
    assertEquals(1, activeCompanies.get(0).getCompanyId());

    List<Integer> events = companyService.getAllEventsByCompany(1);

    assertNotNull(events);
    assertTrue(events.isEmpty());
}

    //end of change for II.2.1

    // II.4.1 Manage Events and Ticket Inventory
    @Test
    void addEventSuccessfully() {
        companyService.openCompany(1, "CompanyA", 100);

        companyService.addEvent(100, 1, 500);

        Company company = companyRepository.findById(1).orElseThrow();
        assertTrue(company.getAssociatedEventIds().contains(500));
    }

    @Test
    void removeEventSuccessfully() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.addEvent(100, 1, 500);

        companyService.removeEvent(100, 1, 500);

        Company company = companyRepository.findById(1).orElseThrow();
        assertFalse(company.getAssociatedEventIds().contains(500));
    }

    @Test
    void companyNotFoundWhenManagingEvents() {
        assertThrows(RuntimeException.class,
                () -> companyService.addEvent(100, 999, 500));
    }

    @Test
    void userNotOwnerWhenManagingEvents() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(SecurityException.class,
                () -> companyService.addEvent(200, 1, 500));
    }

    // II.4.5 View Company Purchase and Order History
    @Test
    void purchaseHistoryDisplayedSuccessfully() {
        companyService.openCompany(1, "CompanyA", 100);

        Company company = companyRepository.findById(1).orElseThrow();
        company.addPurchaseRecord(11);
        company.addPurchaseRecord(12);
        company.addOrderRecord(21);
        companyRepository.save(company);

        List<Integer> purchaseHistory = companyService.getPurchaseHistory(100, 1);
        List<Integer> orderHistory = companyService.getOrderHistory(100, 1);

        assertEquals(2, purchaseHistory.size());
        assertEquals(1, orderHistory.size());
        assertTrue(purchaseHistory.contains(11));
        assertTrue(orderHistory.contains(21));
    }

    @Test
    void noHistoryFound() {
        companyService.openCompany(1, "CompanyA", 100);

        List<Integer> purchaseHistory = companyService.getPurchaseHistory(100, 1);
        List<Integer> orderHistory = companyService.getOrderHistory(100, 1);

        assertTrue(purchaseHistory.isEmpty());
        assertTrue(orderHistory.isEmpty());
    }

    @Test
    void userNotAuthorizedToViewHistory() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(SecurityException.class,
                () -> companyService.getPurchaseHistory(200, 1));
    }

    // II.4.7 Appoint Company Manager
    @Test
    void managerAppointedSuccessfully() {
        companyService.openCompany(1, "CompanyA", 100);

        companyService.appointManager(
                100,
                1,
                200,
                Set.of(CompanyPermission.MANAGE_EVENTS, CompanyPermission.VIEW_HISTORY)
        );

        Company company = companyRepository.findById(1).orElseThrow();

        assertTrue(company.getManagers().contains(200));
        assertTrue(company.getManagerPermissionsView().get(200).contains(CompanyPermission.MANAGE_EVENTS));
        assertTrue(company.getManagerPermissionsView().get(200).contains(CompanyPermission.VIEW_HISTORY));
    }

    @Test
    void nomineeAlreadyManagerOrOwner() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointManager(100, 1, 200, Set.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(IllegalArgumentException.class,
                () -> companyService.appointManager(100, 1, 200, Set.of(CompanyPermission.VIEW_HISTORY)));

        assertThrows(IllegalArgumentException.class,
                () -> companyService.appointManager(100, 1, 100, Set.of(CompanyPermission.MANAGE_EVENTS)));
    }

    @Test
    void userNotAuthorizedToAppointManager() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(SecurityException.class,
                () -> companyService.appointManager(300, 1, 200, Set.of(CompanyPermission.MANAGE_EVENTS)));
    }

    // II.4.8 Appoint Additional Company Owner
    @Test
    void ownerAppointedSuccessfully() {
        companyService.openCompany(1, "CompanyA", 100);

        companyService.appointAdditionalOwner(100, 1, 201);

        Company company = companyRepository.findById(1).orElseThrow();
        assertTrue(company.getOwnerIds().contains(201));
    }

    @Test
    void nomineeAlreadyOwner() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointAdditionalOwner(100, 1, 201);

        assertThrows(IllegalArgumentException.class,
                () -> companyService.appointAdditionalOwner(100, 1, 201));
    }

    // II.4.9 Remove Company Owner Appointment
    @Test
    void ownerRemovedSuccessfully() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointAdditionalOwner(100, 1, 201);

        companyService.removeOwnerAppointment(100, 1, 201);

        Company company = companyRepository.findById(1).orElseThrow();
        assertFalse(company.getOwnerIds().contains(201));
    }

    @Test
    void targetOwnerNotFound() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(IllegalArgumentException.class,
                () -> companyService.removeOwnerAppointment(100, 1, 999));
    }

    @Test
    void userNotAuthorizedToRemoveOwner() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointAdditionalOwner(100, 1, 201);

        assertThrows(SecurityException.class,
                () -> companyService.removeOwnerAppointment(300, 1, 201));
    }

    // II.4.10 Resign from Ownership
    @Test
    void ownershipResignedSuccessfully() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointAdditionalOwner(100, 1, 201);

        companyService.resignOwnership(201, 1);

        Company company = companyRepository.findById(1).orElseThrow();
        assertFalse(company.getOwnerIds().contains(201));
    }

    @Test
    void founderAttemptsResignation() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(IllegalArgumentException.class,
                () -> companyService.resignOwnership(100, 1));
    }

    @Test
    void userNotOwnerResignationDenied() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(SecurityException.class,
                () -> companyService.resignOwnership(999, 1));
    }

    // II.4.11 Modify Manager Permissions
    @Test
    void successfulPermissionUpdate() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointManager(100, 1, 200, Set.of(CompanyPermission.MANAGE_EVENTS));

        companyService.modifyManagerPermissions(
                100,
                1,
                200,
                Set.of(CompanyPermission.VIEW_HISTORY, CompanyPermission.RESPOND_TO_INQUIRIES)
        );

        Company company = companyRepository.findById(1).orElseThrow();
        Set<CompanyPermission> updated = company.getManagerPermissionsView().get(200);

        assertTrue(updated.contains(CompanyPermission.VIEW_HISTORY));
        assertTrue(updated.contains(CompanyPermission.RESPOND_TO_INQUIRIES));
        assertFalse(updated.contains(CompanyPermission.MANAGE_EVENTS));
    }

    @Test
    void unauthorizedUpdate() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointManager(100, 1, 200, Set.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(SecurityException.class,
                () -> companyService.modifyManagerPermissions(
                        300,
                        1,
                        200,
                        Set.of(CompanyPermission.VIEW_HISTORY)
                ));
    }

    @Test
    void wrongManagerPermissionUpdate() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(IllegalArgumentException.class,
                () -> companyService.modifyManagerPermissions(
                        100,
                        1,
                        999,
                        Set.of(CompanyPermission.VIEW_HISTORY)
                ));
    }

    // II.4.12 Remove Manager Appointment
    @Test
    void successfulManagerRemoval() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointManager(100, 1, 200, Set.of(CompanyPermission.MANAGE_EVENTS));

        companyService.removeManagerAppointment(100, 1, 200);

        Company company = companyRepository.findById(1).orElseThrow();
        assertFalse(company.getManagers().contains(200));
        assertFalse(company.getManagerPermissionsView().containsKey(200));
    }

    @Test
    void unauthorizedManagerRemoval() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointManager(100, 1, 200, Set.of(CompanyPermission.MANAGE_EVENTS));

        assertThrows(SecurityException.class,
                () -> companyService.removeManagerAppointment(300, 1, 200));
    }

    @Test
    void wrongManagerRemoval() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(IllegalArgumentException.class,
                () -> companyService.removeManagerAppointment(100, 1, 999));
    }

    // II.4.13 Suspend / Close Production Company
    @Test
    void successfulClosure() {
        companyService.openCompany(1, "CompanyA", 100);

        boolean changed = companyService.closeCompany(100, 1);

        Company company = companyRepository.findById(1).orElseThrow();
        assertTrue(changed);
        assertFalse(company.isOpen());
    }

    @Test
    void unauthorizedClosure() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(SecurityException.class,
                () -> companyService.closeCompany(300, 1));
    }

    @Test
    void alreadyClosed() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.closeCompany(100, 1);

        boolean changedAgain = companyService.closeCompany(100, 1);

        assertFalse(changedAgain);
    }

    // II.4.14 Reopen Production Company
    @Test
    void successfulReopen() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.closeCompany(100, 1);

        boolean changed = companyService.reopenCompany(100, 1);

        Company company = companyRepository.findById(1).orElseThrow();
        assertTrue(changed);
        assertTrue(company.isOpen());
    }

    @Test
    void unauthorizedReopen() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.closeCompany(100, 1);

        assertThrows(SecurityException.class,
                () -> companyService.reopenCompany(300, 1));
    }

    @Test
    void alreadyActive() {
        companyService.openCompany(1, "CompanyA", 100);

        boolean changed = companyService.reopenCompany(100, 1);

        assertFalse(changed);
    }

    // II.4.15 View Roles and Permissions
    @Test
    void viewRolesAndPermissionsSuccessfully() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.appointAdditionalOwner(100, 1, 201);
        companyService.appointManager(
                100,
                1,
                200,
                Set.of(CompanyPermission.MANAGE_EVENTS, CompanyPermission.VIEW_HISTORY)
        );

        CompanyRolesViewDTO rolesView = companyService.viewRolesAndPermissions(100, 1);

        assertEquals(1, rolesView.getCompanyId());
        assertEquals(100, rolesView.getFounderId());
        assertTrue(rolesView.getOwnerIds().contains(100));
        assertTrue(rolesView.getOwnerIds().contains(201));
        assertTrue(rolesView.getManagerPermissions().containsKey(200));
        assertTrue(rolesView.getManagerPermissions().get(200).contains(CompanyPermission.MANAGE_EVENTS));
    }

    @Test
    void emptyRolesDisplayed() {
        companyService.openCompany(1, "CompanyA", 100);

        CompanyRolesViewDTO rolesView = companyService.viewRolesAndPermissions(100, 1);

        assertNotNull(rolesView);
        assertTrue(rolesView.getManagerPermissions().isEmpty());
    }

    @Test
    void unauthorizedAccessToRolesView() {
        companyService.openCompany(1, "CompanyA", 100);

        assertThrows(SecurityException.class,
                () -> companyService.viewRolesAndPermissions(300, 1));
    }

    @Test
    void crossCompanyAccessDenied() {
        companyService.openCompany(1, "CompanyA", 100);
        companyService.openCompany(2, "CompanyB", 200);

        assertThrows(SecurityException.class,
                () -> companyService.viewRolesAndPermissions(100, 2));
    }
}
