package com.sdnah.Ticket_Management_System_;


import com.sdnah.Ticket_Management_System_.Application_Layer.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class company_managment_serivceTest {

    private company_managment_serivce service;
    private FakeRepo repo;

    private static final int COMPANY_ID = 1;
    private static final int FOUNDER = 100;
    private static final int OWNER = 200;
    private static final int MANAGER = 300;
    private static final int USER = 999;

    @BeforeEach
    void setUp() {
        repo = new FakeRepo();
        service = new company_managment_serivce(repo);
        repo.save(new Company(COMPANY_ID, "Main Company", FOUNDER));
    }

    @Test
    void GivenNewCompany_WhenOpenCompany_ThenCompanySaved() {
        service.openCompany(2, "New Company", 500);

        assertTrue(repo.existsById(2));
        assertEquals("New Company", repo.findById(2).get().getCompanyName());
    }

    @Test
    void GivenExistingCompanyId_WhenOpenCompany_ThenFail() {
        assertThrows(IllegalStateException.class,
                () -> service.openCompany(COMPANY_ID, "Duplicate", 500));
    }

    @Test
    void GivenInvalidCompanyData_WhenOpenCompany_ThenFail() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.openCompany(0, "Bad", 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.openCompany(2, "", 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.openCompany(2, "Bad", 0))
        );
    }

   @Test
   void GivenOpenAndClosedCompanies_WhenGetActiveCompanies_ThenOnlyOpenReturned() {
    service.openCompany(2, "Closed Company", 500);
    service.closeCompany(500, 2);

    List<CompanyDTO> active = service.getActiveCompanies();

    assertEquals(1, active.size());
    assertEquals(COMPANY_ID, active.get(0).getCompanyId());
    assertEquals("Main Company", active.get(0).getCompanyName());
    assertTrue(active.get(0).isOpen());
}

    @Test
    void GivenFounder_WhenAddEvent_ThenEventAdded() {
        service.addEvent(FOUNDER, COMPANY_ID, 10);

        assertTrue(repo.findById(COMPANY_ID).get().getAssociatedEventIds().contains(10));
    }

    @Test
    void GivenUnauthorizedUser_WhenAddEvent_ThenFail() {
        assertThrows(SecurityException.class,
                () -> service.addEvent(USER, COMPANY_ID, 10));
    }

    @Test
    void GivenMissingCompany_WhenAddEvent_ThenFail() {
        assertThrows(NoSuchElementException.class,
                () -> service.addEvent(FOUNDER, 999, 10));
    }

    @Test
    void GivenDuplicateEvent_WhenAddEvent_ThenFail() {
        service.addEvent(FOUNDER, COMPANY_ID, 10);

        assertThrows(IllegalArgumentException.class,
                () -> service.addEvent(FOUNDER, COMPANY_ID, 10));
    }

    @Test
    void GivenExistingEvent_WhenRemoveEvent_ThenEventRemoved() {
        service.addEvent(FOUNDER, COMPANY_ID, 10);

        service.removeEvent(FOUNDER, COMPANY_ID, 10);

        assertFalse(repo.findById(COMPANY_ID).get().getAssociatedEventIds().contains(10));
    }

    @Test
    void GivenMissingEvent_WhenRemoveEvent_ThenFail() {
        assertThrows(IllegalArgumentException.class,
                () -> service.removeEvent(FOUNDER, COMPANY_ID, 999));
    }

    @Test
    void GivenUnauthorizedUser_WhenRemoveEvent_ThenFail() {
        service.addEvent(FOUNDER, COMPANY_ID, 10);

        assertThrows(SecurityException.class,
                () -> service.removeEvent(USER, COMPANY_ID, 10));
    }

    @Test
    void GivenFounder_WhenAppointManager_ThenManagerAdded() {
        service.appointManager(
                FOUNDER,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        assertTrue(repo.findById(COMPANY_ID).get().isManager(MANAGER));
    }

    @Test
    void GivenNonFounder_WhenAppointManager_ThenFail() {
        assertThrows(SecurityException.class,
                () -> service.appointManager(
                        USER,
                        COMPANY_ID,
                        MANAGER,
                        EnumSet.of(CompanyPermission.MANAGE_EVENTS)
                ));
    }

    @Test
    void GivenManager_WhenAddEventWithPermission_ThenSuccess() {
        service.appointManager(
                FOUNDER,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        service.addEvent(MANAGER, COMPANY_ID, 20);

        assertTrue(repo.findById(COMPANY_ID).get().getAssociatedEventIds().contains(20));
    }

    @Test
    void GivenManagerWithoutPermission_WhenAddEvent_ThenFail() {
        service.appointManager(
                FOUNDER,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.VIEW_HISTORY)
        );

        assertThrows(SecurityException.class,
                () -> service.addEvent(MANAGER, COMPANY_ID, 20));
    }

    @Test
    void GivenFounder_WhenAppointAdditionalOwner_ThenOwnerAdded() {
        service.appointAdditionalOwner(FOUNDER, COMPANY_ID, OWNER);

        assertTrue(repo.findById(COMPANY_ID).get().isOwner(OWNER));
    }

    @Test
    void GivenNonOwner_WhenAppointAdditionalOwner_ThenFail() {
        assertThrows(SecurityException.class,
                () -> service.appointAdditionalOwner(USER, COMPANY_ID, OWNER));
    }

    @Test
    void GivenExistingOwner_WhenAppointAdditionalOwner_ThenFail() {
        service.appointAdditionalOwner(FOUNDER, COMPANY_ID, OWNER);

        assertThrows(IllegalArgumentException.class,
                () -> service.appointAdditionalOwner(FOUNDER, COMPANY_ID, OWNER));
    }

    @Test
    void GivenOwner_WhenRemoveOwnerAppointment_ThenOwnerRemoved() {
        service.appointAdditionalOwner(FOUNDER, COMPANY_ID, OWNER);

        service.removeOwnerAppointment(FOUNDER, COMPANY_ID, OWNER);

        assertFalse(repo.findById(COMPANY_ID).get().isOwner(OWNER));
    }

    @Test
    void GivenFounderTarget_WhenRemoveOwnerAppointment_ThenFail() {
        service.appointAdditionalOwner(FOUNDER, COMPANY_ID, OWNER);

        assertThrows(IllegalArgumentException.class,
                () -> service.removeOwnerAppointment(OWNER, COMPANY_ID, FOUNDER));
    }

    @Test
    void GivenOwner_WhenResignOwnership_ThenOwnerRemoved() {
        service.appointAdditionalOwner(FOUNDER, COMPANY_ID, OWNER);

        service.resignOwnership(OWNER, COMPANY_ID);

        assertFalse(repo.findById(COMPANY_ID).get().isOwner(OWNER));
    }

    @Test
    void GivenFounder_WhenResignOwnership_ThenFail() {
        assertThrows(IllegalArgumentException.class,
                () -> service.resignOwnership(FOUNDER, COMPANY_ID));
    }

    @Test
    void GivenOwner_WhenModifyManagerPermissions_ThenPermissionsChanged() {
        service.appointManager(
                FOUNDER,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        service.modifyManagerPermissions(
                FOUNDER,
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
        service.appointAdditionalOwner(FOUNDER, COMPANY_ID, OWNER);
        service.appointManager(
                FOUNDER,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        assertThrows(SecurityException.class,
                () -> service.modifyManagerPermissions(
                        OWNER,
                        COMPANY_ID,
                        MANAGER,
                        EnumSet.of(CompanyPermission.VIEW_HISTORY)
                ));
    }

    @Test
    void GivenOwner_WhenRemoveManagerAppointment_ThenManagerRemoved() {
        service.appointManager(
                FOUNDER,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        service.removeManagerAppointment(FOUNDER, COMPANY_ID, MANAGER);

        assertFalse(repo.findById(COMPANY_ID).get().isManager(MANAGER));
    }

    @Test
    void GivenRemovedManager_WhenAddEvent_ThenFail() {
        service.appointManager(
                FOUNDER,
                COMPANY_ID,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );
        service.removeManagerAppointment(FOUNDER, COMPANY_ID, MANAGER);

        assertThrows(SecurityException.class,
                () -> service.addEvent(MANAGER, COMPANY_ID, 30));
    }

    @Test
    void GivenFounder_WhenCloseCompany_ThenCompanyClosed() {
        boolean result = service.closeCompany(FOUNDER, COMPANY_ID);

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
        service.closeCompany(FOUNDER, COMPANY_ID);

        boolean result = service.closeCompany(FOUNDER, COMPANY_ID);

        assertFalse(result);
    }

    @Test
    void GivenClosedCompany_WhenReopenCompany_ThenCompanyOpen() {
        service.closeCompany(FOUNDER, COMPANY_ID);

        boolean result = service.reopenCompany(FOUNDER, COMPANY_ID);

        assertTrue(result);
        assertTrue(repo.findById(COMPANY_ID).get().isOpen());
    }

    @Test
    void GivenNonFounder_WhenReopenCompany_ThenFail() {
        service.closeCompany(FOUNDER, COMPANY_ID);

        assertThrows(SecurityException.class,
                () -> service.reopenCompany(USER, COMPANY_ID));
    }

    

    @Test
    void GivenNonOwner_WhenViewRolesAndPermissions_ThenFail() {
        assertThrows(SecurityException.class,
                () -> service.viewRolesAndPermissions(USER, COMPANY_ID));
    }

    private static class FakeRepo implements ICompanyRepository {
        private final Map<Integer, Company> companies = new ConcurrentHashMap<>();

        public void save(Company company) {
            companies.put(company.getCompanyId(), company);
        }

        public Optional<Company> findById(int companyId) {
            return Optional.ofNullable(companies.get(companyId));
        }

        public Collection<Company> findAll() {
            return new ArrayList<>(companies.values());
        }

        public void deleteById(int companyId) {
            companies.remove(companyId);
        }

        public boolean existsById(int companyId) {
            return companies.containsKey(companyId);
        }
    }
}