package com.sdnah.Ticket_Management_System_.User.ConcurrencyTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Company.CompanyRoleService;
import com.sdnah.Ticket_Management_System_.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.User.IntegrationTests.testconfig.TestConfig;

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
    private AuthTokenService authTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemAdminRepository systemAdminRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static class Outcome {
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
        final AtomicReference<Throwable> firstError = new AtomicReference<>();
    }

    @BeforeEach
    void cleanDb() {
        jdbcTemplate.update("DELETE FROM member_company_roles");
        systemAdminRepository.deleteAll();
        userRepository.deleteAll();
        // JWTs are stateless — no token table to clean.
    }

    @AfterEach
    void afterCleanDb() {
        jdbcTemplate.update("DELETE FROM member_company_roles");
        systemAdminRepository.deleteAll();
        userRepository.deleteAll();
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
                    outcome.firstError.compareAndSet(null, t);
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

    private String firstErrorMessage(Outcome outcome) {
        Throwable first = outcome.firstError.get();
        if (first == null) {
            return "none";
        }

        return first.getClass().getSimpleName() + ": " + first.getMessage();
    }

    @Test
    @DisplayName("Concurrent registration with same username: only one succeeds")
    void concurrentRegistration_SameUsername_OnlyOneSucceeds() throws Exception {
        // Arrange
        String username = "race_user_" + UUID.randomUUID();

        // Act
        Outcome outcome = runConcurrently(20, () -> userService.register(
                username,
                "password123",
                username + "@example.com",
                "0501234567",
                VerificationMethod.EMAIL));

        // Assert
        assertEquals(1, outcome.successes.get(),
                "expected exactly one successful registration, first error: " + firstErrorMessage(outcome));
        assertEquals(19, outcome.failures.get(), "all other attempts should fail");
        assertTrue(userRepository.existsByUsername(username));

        long usernameRows = userRepository.findAll().stream()
                .filter(m -> username.equals(m.getUsername()))
                .count();

        assertEquals(1, usernameRows);
    }

    @Test
    @DisplayName("Concurrent assignOwner on same target: only one role is persisted")
    void concurrentAssignOwner_SameTarget_OnlyOneSucceeds() throws Exception {
        // Arrange
        int companyId = Math.abs(UUID.randomUUID().hashCode());
        if (companyId == 0) {
            companyId = 1;
        }

        String ownerUsername = "ownerUser_" + UUID.randomUUID();
        String targetId = "target-" + UUID.randomUUID();
        String targetUsername = "targetUser_" + UUID.randomUUID();

        String ownerId = userService.register(
                ownerUsername,
                "password123",
                ownerUsername + "@example.com",
                "0501234567",
                VerificationMethod.EMAIL);

        Member ownerMember = userRepository.findById(ownerId).orElseThrow();
        ownerMember.setVerified(true);
        ownerMember.addCompanyRole(new CompanyRoleAssignment(
                companyId,
                ownerId,
                CompanyRoleType.OWNER,
                new HashSet<>()));
        userRepository.saveAndFlush(ownerMember);

        Member target = new Member(targetId, targetUsername, "hash");
        target.setVerified(true);
        userRepository.saveAndFlush(target);

        String ownerToken = userService.login(ownerUsername, "password123");

        final int finalCompanyId = companyId;

        // Act
        Outcome outcome = runConcurrently(15,
                () -> companyRoleService.assignOwner(ownerToken, finalCompanyId, targetId));

        // Assert
        assertEquals(1, outcome.successes.get(),
                "expected one success, first error: " + firstErrorMessage(outcome));
        assertEquals(14, outcome.failures.get(), "the rest should fail with already-assigned");

        Member reloaded = userRepository.findById(targetId).orElseThrow();

        assertTrue(reloaded.isOwnerInCompany(finalCompanyId));

        long ownerRoles = reloaded.getCompanyRoles().stream()
                .filter(r -> r.getCompanyId() == finalCompanyId && r.isOwner())
                .count();

        assertEquals(1, ownerRoles, "must end with exactly one OWNER role for that company");
    }

    @Test
    @DisplayName("Concurrent assign_system_admin on same target: only one promotion succeeds")
    void concurrentAssignSystemAdmin_SameTarget_OnlyOneSucceeds() throws Exception {
        // Arrange
        String adminId = "admin-" + UUID.randomUUID();
        String adminUsername = "adminUser-" + UUID.randomUUID();
        String targetId = "target-" + UUID.randomUUID();

        Member baseAdmin = new Member(adminId, adminUsername, "hash");
        baseAdmin.setVerified(true);

        systemAdminRepository.saveAndFlush(new System_admin(baseAdmin, "System"));

        Member target = new Member(targetId, "targetUser-" + UUID.randomUUID(), "hash");
        target.setVerified(true);
        userRepository.saveAndFlush(target);

        // Issue a real JWT for the admin — same logic as login, no DB row needed.
        String adminToken = authTokenService.generateToken(adminUsername);

        assertTrue(authTokenService.validateToken(adminToken),
                "precondition failed: admin JWT did not validate");

        assertTrue(systemAdminRepository.existsById(adminId),
                "precondition failed: base admin was not saved");

        // Act
        Outcome outcome = runConcurrently(10,
                () -> systemAdminService.assign_system_admin(adminToken, targetId));

        // Assert
        assertEquals(1, outcome.successes.get(),
                "exactly one promotion should succeed, first error: " + firstErrorMessage(outcome));
        assertEquals(9, outcome.failures.get(), "the rest should fail");

        assertTrue(systemAdminRepository.existsById(targetId), "target should be a system admin");

        long adminRows = systemAdminRepository.findAll().stream()
                .filter(a -> a.getMemberId().equals(targetId))
                .count();

        assertEquals(1, adminRows, "exactly one System_admin row for target");
    }

    @Test
    @DisplayName("Concurrent registration with different usernames: all succeed")
    void concurrentRegistration_DifferentUsernames_AllSucceed() throws Exception {
        // Arrange
        int n = 20;
        String prefix = "user_" + UUID.randomUUID() + "_";

        // Act
        Outcome outcome = runConcurrently(n, () -> {
            String username = prefix + UUID.randomUUID();

            userService.register(
                    username,
                    "password123",
                    username + "@example.com",
                    "0501234567",
                    VerificationMethod.EMAIL);
        });

        // Assert
        assertEquals(n, outcome.successes.get(),
                "distinct usernames should all succeed, first error: " + firstErrorMessage(outcome));
        assertEquals(0, outcome.failures.get(), "distinct usernames should not fail");
    }
}