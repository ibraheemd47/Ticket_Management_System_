package com.sdnah.Ticket_Management_System_.Lottery.ConcurrencyTests;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.LotteryService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LotteryService — Concurrency Tests")
public class LotteryConcurrencyTest {

    @Autowired private LotteryService lotteryService;
    @Autowired private UserService userService;
    @Autowired private LotteryRepository lotteryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private company_managment_serivce companyService;

    private String ownerToken;
    private int companyId;
    private UUID eventId;
    private UUID lotteryId;

    private static class Outcome {
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failures  = new AtomicInteger();
        final AtomicReference<Throwable> firstError = new AtomicReference<>();
    }

    @BeforeEach
    void setUp() {
        eventId   = UUID.randomUUID();
        companyId = Math.abs(UUID.randomUUID().hashCode() % 10000) + 1;

        String ownerId = userService.register(
                "owner_" + UUID.randomUUID(), "123456",
                "owner" + UUID.randomUUID() + "@test.com",
                "0501234567", VerificationMethod.EMAIL);
        String ownerUsername = userRepository.findById(ownerId).orElseThrow().getUsername();
        userService.verifyAccount(ownerUsername, "123456");
        ownerToken = userService.login(ownerUsername, "123456");

        // שם ייחודי לכל טסט
        companyService.openCompany(ownerToken, companyId, "Test Company " + UUID.randomUUID());

        LotteryDTO dto = lotteryService.createLottery(
                ownerToken, eventId, companyId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
        lotteryId = dto.getId();
    }

    private Outcome runConcurrently(int threads, Runnable action) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
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
        assertTrue(done.await(20, TimeUnit.SECONDS), "timeout");
        pool.shutdownNow();
        return outcome;
    }

    @Test
    @DisplayName("Concurrent registration same member: only one succeeds")
    void concurrentRegistration_SameMember_OnlyOneSucceeds() throws InterruptedException {
        String memberId = userService.register(
                "member_" + UUID.randomUUID(), "123456",
                "member" + UUID.randomUUID() + "@test.com",
                "0501234568", VerificationMethod.EMAIL);
        String memberUsername = userRepository.findById(memberId).orElseThrow().getUsername();
        userService.verifyAccount(memberUsername, "123456");
        String memberToken = userService.login(memberUsername, "123456");

        Outcome outcome = runConcurrently(10,
                () -> lotteryService.registerToLottery(memberToken, lotteryId));

        // בדוק שלא נרשם יותר מפעם אחת
        var lottery = lotteryRepository.findById(lotteryId).orElseThrow();
        assertTrue(lottery.getEntries().size() <= 1, 
                "member should be registered at most once, but was: " + lottery.getEntries().size());
        assertTrue(outcome.successes.get() >= 1, "at least one should succeed");
    }

    @Test
    @DisplayName("Concurrent registration different members: all succeed")
    void concurrentRegistration_DifferentMembers_AllSucceed() throws InterruptedException {
        int n = 10;

        Outcome outcome = runConcurrently(n, () -> {
            String id = userService.register(
                    "member_" + UUID.randomUUID(), "123456",
                    "member" + UUID.randomUUID() + "@test.com",
                    "0501234568", VerificationMethod.EMAIL);
            String username = userRepository.findById(id).orElseThrow().getUsername();
            userService.verifyAccount(username, "123456");
            String token = userService.login(username, "123456");
            lotteryService.registerToLottery(token, lotteryId);
        });

        assertEquals(n, outcome.successes.get(), "all different members should register");
        assertEquals(0, outcome.failures.get());

        var lottery = lotteryRepository.findById(lotteryId).orElseThrow();
        assertEquals(n, lottery.getEntries().size());
    }
}