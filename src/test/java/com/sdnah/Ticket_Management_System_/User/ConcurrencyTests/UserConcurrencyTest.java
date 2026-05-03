package com.sdnah.Ticket_Management_System_.User.ConcurrencyTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Application_Layer.CompanyRoleService;
import com.sdnah.Ticket_Management_System_.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.User.IntegrationTests.testconfig.TestConfig;

/**
 * Concurrency tests for the User module. Each test fires N threads against the
 * same logical resource (same username / same target member) and asserts the
 * service serialises them correctly: exactly one winner, the rest get a
 * deterministic business error rather than a corrupted DB row.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class UserConcurrencyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyRoleService companyRoleService;

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private SystemAdminRepository systemAdminRepository;

    @BeforeEach
    void cleanDb() {
        tokenRepository.deleteAll();
        systemAdminRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void afterCleanDb() {
        tokenRepository.deleteAll();
        systemAdminRepository.deleteAll();
        userRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static class Outcome {
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
    }

    private Outcome runConcurrently(int threads, Runnable action) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Outcome outcome = new Outcome();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    action.run();
                    outcome.successes.incrementAndGet();
                } catch (Throwable t) {
                    outcome.failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(20, TimeUnit.SECONDS);
        pool.shutdownNow();
        assertTrue(finished, "concurrent workload did not finish within timeout");
        return outcome;
    }

    private <T> List<Future<T>> submitAll(ExecutorService pool, int n, Callable<T> task) {
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> pool.submit(task))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Register: same username from N threads -> exactly one persisted row
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent registration with same username: only one succeeds")
    void concurrentRegistration_SameUsername_OnlyOneSucceeds() throws Exception {
        String username = "race_user_" + UUID.randomUUID();

        Outcome outcome = runConcurrently(20, () -> userService.register(
                username,
                "password123",
                username + "@example.com",
                "0501234567",
                VerificationMethod.EMAIL));

        assertEquals(1, outcome.successes.get(), "expected exactly one successful registration");
        assertEquals(19, outcome.failures.get(), "all other attempts should fail");
        assertTrue(userRepository.existsByUsername(username));
        assertEquals(1, userRepository.findAll().stream()
                .filter(m -> username.equals(m.getUsername()))
                .count());
    }

    // -------------------------------------------------------------------------
    // assignOwner: N threads racing to assign owner to same target -> exactly one wins
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent assignOwner on same target: only one role is persisted")
    void concurrentAssignOwner_SameTarget_OnlyOneSucceeds() throws Exception {
        String companyId = "co-" + UUID.randomUUID();
        String ownerId = "owner-" + UUID.randomUUID();
        String targetId = "target-" + UUID.randomUUID();
        String ownerToken = "tok-owner-" + UUID.randomUUID();

        Member ownerMember = new Member(ownerId, "ownerUser", "hash");
        ownerMember.addCompanyRole(new CompanyRoleAssignment(
                companyId, ownerId, CompanyRoleType.OWNER, new HashSet<>()));
        userRepository.save(ownerMember);

        userRepository.save(new Member(targetId, "targetUser", "hash"));

        tokenRepository.save(new AuthToken(ownerToken, ownerId, LocalDateTime.now().plusHours(1)));

        Outcome outcome = runConcurrently(15,
                () -> companyRoleService.assignOwner(ownerToken, companyId, targetId));

        assertEquals(1, outcome.successes.get(), "exactly one assignOwner should succeed");
        assertEquals(14, outcome.failures.get(), "the rest should fail with already-assigned");

        Member reloaded = userRepository.findById(targetId).orElseThrow();
        assertTrue(reloaded.isOwnerInCompany(companyId));
        long ownerRoles = reloaded.getCompanyRoles().stream()
                .filter(r -> r.getCompanyId().equals(companyId) && r.isOwner())
                .count();
        assertEquals(1, ownerRoles, "must end with exactly one OWNER role for that company");
    }

    // -------------------------------------------------------------------------
    // assign_system_admin: N threads racing to promote same member -> exactly one wins
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent assign_system_admin on same target: only one promotion succeeds")
    void concurrentAssignSystemAdmin_SameTarget_OnlyOneSucceeds() throws Exception {
        String adminId = "admin-" + UUID.randomUUID();
        String targetId = "target-" + UUID.randomUUID();
        String adminToken = "admin-tok-" + UUID.randomUUID();

        Member baseAdmin = new Member(adminId, "adminUser", "hash");
        systemAdminRepository.save(new System_admin(baseAdmin, "System"));

        userRepository.save(new Member(targetId, "targetUser", "hash"));

        tokenRepository.save(new AuthToken(adminToken, adminId, LocalDateTime.now().plusHours(1)));

        Outcome outcome = runConcurrently(10,
                () -> systemAdminService.assign_system_admin(adminToken, targetId));

        assertEquals(1, outcome.successes.get(), "exactly one promotion should succeed");
        assertEquals(9, outcome.failures.get(), "the rest should fail");

        assertTrue(systemAdminRepository.existsById(targetId), "target should be a system admin");
        long adminRows = systemAdminRepository.findAll().stream()
                .filter(a -> a.getMemberId().equals(targetId))
                .count();
        assertEquals(1, adminRows, "exactly one System_admin row for target");
    }

    // -------------------------------------------------------------------------
    // Different usernames in parallel: all should succeed (no false-sharing)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent registration with different usernames: all succeed")
    void concurrentRegistration_DifferentUsernames_AllSucceed() throws Exception {
        int n = 20;
        String prefix = "user_" + UUID.randomUUID() + "_";
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < n; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    userService.register(
                            prefix + idx,
                            "password123",
                            prefix + idx + "@example.com",
                            "0501234567",
                            VerificationMethod.EMAIL);
                    successes.incrementAndGet();
                } catch (Throwable ignored) {
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
        assertEquals(n, successes.get(), "distinct usernames should not block each other");
    }
}
