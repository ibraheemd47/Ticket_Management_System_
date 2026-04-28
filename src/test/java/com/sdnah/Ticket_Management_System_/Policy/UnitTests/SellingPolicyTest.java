package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;

@DisplayName("SellingPolicy — Domain Unit Tests")
class SellingPolicyTest {

    // =========================================================================
    // 1. Constructor + getters
    // =========================================================================

    @Test
    @DisplayName("Given valid data, when SellingPolicy is constructed, then id is stored correctly")
    void givenValidData_WhenConstructed_ThenIdIsStoredCorrectly() {
        // Arrange
        int policyId = 1;

        // Act
        SellingPolicy policy = new SellingPolicy(
                policyId,
                "Regular selling",
                SellingPolicy.SellingType.REGULAR,
                100
        );

        // Assert
        assertEquals(policyId, policy.getPolicyId());
    }

    @Test
    @DisplayName("Given valid data, when SellingPolicy is constructed, then description is stored correctly")
    void givenValidData_WhenConstructed_ThenDescriptionIsStoredCorrectly() {
        // Arrange
        String description = "Lottery selling";

        // Act
        SellingPolicy policy = new SellingPolicy(
                1,
                description,
                SellingPolicy.SellingType.LOTTERY,
                100
        );

        // Assert
        assertEquals(description, policy.getDescription());
    }

    @Test
    @DisplayName("Given valid data, when SellingPolicy is constructed, then event id is stored correctly")
    void givenValidData_WhenConstructed_ThenEventIdIsStoredCorrectly() {
        // Arrange
        int eventId = 100;

        // Act
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular selling",
                SellingPolicy.SellingType.REGULAR,
                eventId
        );

        // Assert
        assertEquals(eventId, policy.getEventId());
    }

    @Test
    @DisplayName("Given SellingPolicy, when isValid is called, then returns true")
    void givenSellingPolicy_WhenIsValidCalled_ThenReturnsTrue() {
        // Arrange
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular selling",
                SellingPolicy.SellingType.REGULAR,
                100
        );

        // Act
        boolean result = policy.isValid();

        // Assert
        assertTrue(result);
    }

    // =========================================================================
    // 2. isSelectionAllowed — REGULAR
    // =========================================================================

    @Test
    @DisplayName("Given REGULAR selling policy and member, when isSelectionAllowed is called, then returns true")
    void givenRegularSellingPolicyAndMember_WhenIsSelectionAllowedCalled_ThenReturnsTrue() {
        // Arrange
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular selling",
                SellingPolicy.SellingType.REGULAR,
                100
        );

        // Act
        boolean result = policy.isSelectionAllowed(true);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given REGULAR selling policy and guest, when isSelectionAllowed is called, then returns true")
    void givenRegularSellingPolicyAndGuest_WhenIsSelectionAllowedCalled_ThenReturnsTrue() {
        // Arrange
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular selling",
                SellingPolicy.SellingType.REGULAR,
                100
        );

        // Act
        boolean result = policy.isSelectionAllowed(false);

        // Assert
        assertTrue(result);
    }

    // =========================================================================
    // 3. isSelectionAllowed — LOTTERY
    // =========================================================================

    @Test
    @DisplayName("Given LOTTERY selling policy and member, when isSelectionAllowed is called, then returns true")
    void givenLotterySellingPolicyAndMember_WhenIsSelectionAllowedCalled_ThenReturnsTrue() {
        // Arrange
        SellingPolicy policy = new SellingPolicy(
                2,
                "Lottery selling",
                SellingPolicy.SellingType.LOTTERY,
                200
        );

        // Act
        boolean result = policy.isSelectionAllowed(true);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given LOTTERY selling policy and guest, when isSelectionAllowed is called, then returns false")
    void givenLotterySellingPolicyAndGuest_WhenIsSelectionAllowedCalled_ThenReturnsFalse() {
        // Arrange
        SellingPolicy policy = new SellingPolicy(
                2,
                "Lottery selling",
                SellingPolicy.SellingType.LOTTERY,
                200
        );

        // Act
        boolean result = policy.isSelectionAllowed(false);

        // Assert
        assertFalse(result);
    }

    // =========================================================================
    // 4. Concurrency tests
    // =========================================================================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Given many guests and LOTTERY policy, when selection is checked concurrently, then all are rejected")
    void givenManyGuestsAndLotteryPolicy_WhenSelectionCheckedConcurrently_ThenAllAreRejected() throws Exception {
        // Arrange
        SellingPolicy policy = new SellingPolicy(
                3,
                "Lottery race test",
                SellingPolicy.SellingType.LOTTERY,
                300
        );

        int numberOfThreads = 50;
        int numberOfTasks = 300;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < numberOfTasks; i++) {
            results.add(executor.submit(() -> {
                startSignal.await();
                return policy.isSelectionAllowed(false);
            }));
        }

        // Act
        startSignal.countDown();

        // Assert
        for (Future<Boolean> result : results) {
            assertFalse(result.get());
        }

        executor.shutdownNow();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Given many members and LOTTERY policy, when selection is checked concurrently, then all are allowed")
    void givenManyMembersAndLotteryPolicy_WhenSelectionCheckedConcurrently_ThenAllAreAllowed() throws Exception {
        // Arrange
        SellingPolicy policy = new SellingPolicy(
                4,
                "Lottery member race test",
                SellingPolicy.SellingType.LOTTERY,
                400
        );

        int numberOfThreads = 50;
        int numberOfTasks = 300;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < numberOfTasks; i++) {
            results.add(executor.submit(() -> {
                startSignal.await();
                return policy.isSelectionAllowed(true);
            }));
        }

        // Act
        startSignal.countDown();

        // Assert
        for (Future<Boolean> result : results) {
            assertTrue(result.get());
        }

        executor.shutdownNow();
    }
}