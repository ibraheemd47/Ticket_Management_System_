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
        private static final String FOUNDER = "1";
        private static final String OWNER = "2";
        private static final String MANAGER = "3";
        private static final String USER = "4";

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
                                                () -> new Company(1, "A", "")));
        }

        @Test
        void GivenOwner_WhenAddRemoveAndValidateEvent_ThenSuccess() {
                UUID eventId = UUID.randomUUID();

                company.addEventId(FOUNDER, eventId);

                assertEquals(List.of(eventId), company.getAssociatedEventIds());
                assertDoesNotThrow(() -> company.validateEventBelongsToCompany(eventId));

                company.removeEvent(FOUNDER, eventId);

                assertTrue(company.getAssociatedEventIds().isEmpty());
        }

        @Test
        void GivenInvalidEventOperations_WhenCalled_ThenFail() {
                UUID eventId = UUID.randomUUID();
                UUID otherEvent = UUID.randomUUID();

                company.addEventId(FOUNDER, eventId);

                assertAll(
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.addEventId(FOUNDER, eventId)),
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.removeEvent(FOUNDER, otherEvent)),
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.validateEventBelongsToCompany(otherEvent)),
                                () -> assertThrows(SecurityException.class,
                                                () -> company.addEventId(USER, UUID.randomUUID())));
        }

        @Test
        void GivenManagerWithPermission_WhenManageEvent_ThenSuccess() {
                UUID eventId = UUID.randomUUID();
                UUID event2 = UUID.randomUUID();

                company.appointManager(
                                FOUNDER,
                                MANAGER,
                                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

                company.addEventId(FOUNDER, eventId);

                assertDoesNotThrow(() -> company.defineEventLayout(MANAGER, eventId, "map"));

                company.addEventId(MANAGER, event2);

                assertTrue(company.getAssociatedEventIds().contains(event2));
        }

        @Test
        void GivenManagerWithoutPermission_WhenManageEvent_ThenFail() {
                UUID eventId = UUID.randomUUID();

                company.addEventId(FOUNDER, eventId);
                company.appointManager(
                                FOUNDER,
                                MANAGER,
                                EnumSet.of(CompanyPermission.VIEW_HISTORY));

                assertThrows(SecurityException.class,
                                () -> company.defineEventLayout(MANAGER, eventId, "map"));
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
                                                () -> company.appointAdditionalOwner(USER, "5")),
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.appointAdditionalOwner(FOUNDER, OWNER)),
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.removeOwnerAppointment(OWNER, FOUNDER)),
                                () -> assertThrows(SecurityException.class,
                                                () -> company.resignOwnership(USER)),
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.resignOwnership(FOUNDER)));
        }

        @Test
        void GivenOwner_WhenAppointModifyAndRemoveManager_ThenSuccess() {
                company.appointManager(
                                FOUNDER,
                                MANAGER,
                                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

                assertTrue(company.isManager(MANAGER));
                assertTrue(company.managerHasPermission(MANAGER, CompanyPermission.MANAGE_EVENTS));

                company.modifyManagerPermissions(
                                FOUNDER,
                                MANAGER,
                                EnumSet.of(CompanyPermission.VIEW_HISTORY));

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
                                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

                assertAll(
                                () -> assertThrows(SecurityException.class,
                                                () -> company.appointManager(USER, "8",
                                                                EnumSet.of(CompanyPermission.MANAGE_EVENTS))),
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.appointManager(FOUNDER, MANAGER,
                                                                EnumSet.of(CompanyPermission.VIEW_HISTORY))),
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.appointManager(FOUNDER, OWNER, null)),
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.modifyManagerPermissions(FOUNDER, "999",
                                                                EnumSet.of(CompanyPermission.VIEW_HISTORY))),
                                () -> assertThrows(IllegalArgumentException.class,
                                                () -> company.modifyManagerPermissions(FOUNDER, MANAGER, null)));
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
                UUID eventId = UUID.randomUUID();

                company.closeCompany(FOUNDER);

                assertThrows(IllegalStateException.class,
                                () -> company.addEventId(FOUNDER, eventId));

                assertTrue(company.getAssociatedEventIds().isEmpty());
        }

        @Test
        void GivenReturnedLists_WhenModifiedExternally_ThenCompanyStateNotChanged() {
                UUID eventId = UUID.randomUUID();

                company.addEventId(FOUNDER, eventId);

                List<UUID> events = company.getAssociatedEventIds();
                events.add(UUID.randomUUID());

                assertEquals(List.of(eventId), company.getAssociatedEventIds());
                assertThrows(UnsupportedOperationException.class,
                                () -> company.getOwnerIds().add("999"));
        }

        @Test
        @Timeout(10)
        void GivenConcurrentAddSameEvent_WhenRun_ThenOnlyOneSucceeds() throws Exception {
                UUID eventId = UUID.randomUUID();

                int threads = 40;
                ExecutorService executor = Executors.newFixedThreadPool(threads);
                CountDownLatch start = new CountDownLatch(1);
                CountDownLatch done = new CountDownLatch(threads);
                AtomicInteger success = new AtomicInteger();

                for (int i = 0; i < threads; i++) {
                        executor.submit(() -> {
                                try {
                                        start.await();
                                        company.addEventId(FOUNDER, eventId);
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
                assertEquals(List.of(eventId), company.getAssociatedEventIds());
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
                                                        EnumSet.of(CompanyPermission.MANAGE_EVENTS));
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

        @Test
        void GivenAdminCloseCompany_WhenCalled_ThenCompanyClosedAndRolesCleared() {
                company.appointAdditionalOwner(FOUNDER, OWNER);
                company.appointManager(
                                FOUNDER,
                                MANAGER,
                                EnumSet.of(CompanyPermission.MANAGE_EVENTS));

                boolean changed = company.adminCloseCompany();

                assertTrue(changed);
                assertFalse(company.isOpen());
                assertTrue(company.getOwnerIds().isEmpty());
                assertTrue(company.getManagers().isEmpty());
        }

        @Test
        void GivenAlreadyClosedCompany_WhenAdminCloseCompany_ThenReturnFalse() {
                company.adminCloseCompany();

                boolean changedAgain = company.adminCloseCompany();

                assertFalse(changedAgain);
        }
}