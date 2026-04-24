package com.sdnah.Ticket_Management_System_;


import org.junit.jupiter.api.*;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Company.CompanyPermission;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CompanyTest {

    private Company company;
    private static final int FOUNDER = 1;
    private static final int OWNER = 2;
    private static final int MANAGER = 3;
    private static final int USER = 4;

    @BeforeEach
    void setUp() {
        company = new Company(10, "Test Company", FOUNDER);
    }

    @Test
    void GivenValidData_WhenCreateCompany_ThenFounderIsOwnerAndCompanyOpen() {
        assertEquals(10, company.getCompanyId());
        assertEquals("Test Company", company.getCompanyName());
        assertTrue(company.isOpen());
        assertTrue(company.isFounder(FOUNDER));
        assertTrue(company.isOwner(FOUNDER));
    }

    @Test
    void GivenInvalidData_WhenCreateCompany_ThenFail() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Company(0, "A", FOUNDER)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Company(1, "", FOUNDER)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Company(1, null, FOUNDER)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Company(1, "A", 0))
        );
    }

    @Test
    void GivenOwner_WhenAddRemoveAndValidateEvent_ThenSuccess() {
        company.addEventId(FOUNDER, 100);

        assertEquals(List.of(100), company.getAssociatedEventIds());
        assertDoesNotThrow(() -> company.validateEventBelongsToCompany(100));

        company.removeEvent(100);

        assertTrue(company.getAssociatedEventIds().isEmpty());
    }

    @Test
    void GivenInvalidEventOperations_WhenCalled_ThenFail() {
        company.addEventId(FOUNDER, 100);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.addEventId(FOUNDER, 100)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.removeEvent(999)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.validateEventBelongsToCompany(999)),
                () -> assertThrows(SecurityException.class,
                        () -> company.addEventId(USER, 200))
        );
    }

    @Test
    void GivenManagerWithPermission_WhenManageEvent_ThenSuccess() {
        company.addEventId(FOUNDER, 100);
        company.appointManager(
                FOUNDER,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        assertDoesNotThrow(() ->
                company.defineEventLayout(MANAGER, 100, "map"));
        company.addEventId(MANAGER, 101);

        assertTrue(company.getAssociatedEventIds().contains(101));
    }

    @Test
    void GivenManagerWithoutPermission_WhenManageEvent_ThenFail() {
        company.addEventId(FOUNDER, 100);
        company.appointManager(
                FOUNDER,
                MANAGER,
                EnumSet.of(CompanyPermission.VIEW_HISTORY)
        );

        assertThrows(SecurityException.class,
                () -> company.defineEventLayout(MANAGER, 100, "map"));
    }

    @Test
    void GivenOwner_WhenAddAndViewHistory_ThenSuccess() {
        company.addPurchaseRecord(11);
        company.addOrderRecord(22);

        assertEquals(List.of(11), company.getPurchaseHistoryIds(FOUNDER));
        assertEquals(List.of(22), company.getOrderHistoryIds(FOUNDER));
    }

    @Test
    void GivenUnauthorizedUser_WhenViewHistory_ThenFail() {
        assertThrows(SecurityException.class,
                () -> company.getPurchaseHistoryIds(USER));
    }

    @Test
    void GivenOwner_WhenAppointAndRemoveOwner_ThenSuccess() {
        company.appointAdditionalOwner(FOUNDER, OWNER);

        assertTrue(company.isOwner(OWNER));

        company.removeOwnerAppointment(FOUNDER, OWNER);

        assertFalse(company.isOwner(OWNER));
    }

    @Test
    void GivenInvalidOwnerOperations_WhenCalled_ThenFail() {
        company.appointAdditionalOwner(FOUNDER, OWNER);

        assertAll(
                () -> assertThrows(SecurityException.class,
                        () -> company.appointAdditionalOwner(USER, 5)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.appointAdditionalOwner(FOUNDER, OWNER)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.removeOwnerAppointment(OWNER, FOUNDER)),
                () -> assertThrows(SecurityException.class,
                        () -> company.resignOwnership(USER)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.resignOwnership(FOUNDER))
        );
    }

    @Test
    void GivenOwner_WhenAppointModifyAndRemoveManager_ThenSuccess() {
        company.appointManager(
                FOUNDER,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        assertTrue(company.isManager(MANAGER));
        assertTrue(company.managerHasPermission(MANAGER, CompanyPermission.MANAGE_EVENTS));

        company.modifyManagerPermissions(
                FOUNDER,
                MANAGER,
                EnumSet.of(CompanyPermission.VIEW_HISTORY)
        );

        assertFalse(company.managerHasPermission(MANAGER, CompanyPermission.MANAGE_EVENTS));
        assertTrue(company.managerHasPermission(MANAGER, CompanyPermission.VIEW_HISTORY));

        company.removeManagerAppointment(FOUNDER, MANAGER);

        assertFalse(company.isManager(MANAGER));
    }

    @Test
    void GivenInvalidManagerOperations_WhenCalled_ThenFail() {
        company.appointManager(
                FOUNDER,
                MANAGER,
                EnumSet.of(CompanyPermission.MANAGE_EVENTS)
        );

        assertAll(
                () -> assertThrows(SecurityException.class,
                        () -> company.appointManager(USER, 8, EnumSet.of(CompanyPermission.MANAGE_EVENTS))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.appointManager(FOUNDER, MANAGER, EnumSet.of(CompanyPermission.VIEW_HISTORY))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.appointManager(FOUNDER, OWNER, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.modifyManagerPermissions(FOUNDER, 999, EnumSet.of(CompanyPermission.VIEW_HISTORY))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> company.modifyManagerPermissions(FOUNDER, MANAGER, null))
        );
    }

    @Test
    void GivenFounder_WhenCloseAndReopenCompany_ThenSuccess() {
        assertTrue(company.closeCompany(FOUNDER));
        assertFalse(company.isOpen());

        assertTrue(company.reopenCompany(FOUNDER));
        assertTrue(company.isOpen());
    }

    @Test
    void GivenNonFounder_WhenCloseOrReopenCompany_ThenFail() {
        assertThrows(SecurityException.class,
                () -> company.closeCompany(USER));

        company.closeCompany(FOUNDER);

        assertThrows(SecurityException.class,
                () -> company.reopenCompany(USER));
    }

    @Test
    void GivenClosedCompany_WhenTryingManagementAction_ThenFail() {
        company.closeCompany(FOUNDER);

        assertThrows(IllegalStateException.class,
                () -> company.addEventId(FOUNDER, 100));

        assertTrue(company.getAssociatedEventIds().isEmpty());
    }

    @Test
    void GivenReturnedLists_WhenModifiedExternally_ThenCompanyStateNotChanged() {
        company.addEventId(FOUNDER, 100);

        List<Integer> events = company.getAssociatedEventIds();
        events.add(999);

        assertEquals(List.of(100), company.getAssociatedEventIds());
        assertThrows(UnsupportedOperationException.class,
                () -> company.getOwnerIds().add(999));
    }

    @Test
    @Timeout(10)
    void GivenConcurrentAddSameEvent_WhenRun_ThenOnlyOneSucceeds() throws Exception {
        int threads = 40;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    company.addEventId(FOUNDER, 777);
                    success.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        executor.shutdownNow();

        assertEquals(1, success.get());
        assertEquals(List.of(777), company.getAssociatedEventIds());
    }

    @Test
    @Timeout(10)
    void GivenConcurrentAppointSameManager_WhenRun_ThenOnlyOneSucceeds() throws Exception {
        int threads = 40;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    company.appointManager(
                            FOUNDER,
                            MANAGER,
                            EnumSet.of(CompanyPermission.MANAGE_EVENTS)
                    );
                    success.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        executor.shutdownNow();

        assertEquals(1, success.get());
        assertTrue(company.isManager(MANAGER));
    }
}