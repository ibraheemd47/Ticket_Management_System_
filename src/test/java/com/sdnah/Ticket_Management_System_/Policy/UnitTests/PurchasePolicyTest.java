package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;

@DisplayName("PurchasePolicy — Domain Unit Tests")
class PurchasePolicyTest {

    private PurchasePolicy purchasePolicy;

    @BeforeEach
    void setUp() {
        purchasePolicy = new PurchasePolicy(10, "Default purchase policy", 1000);
    }

    // =========================================================================
    // 1. Constructor + getters
    // =========================================================================

    @Test
    @DisplayName("Given valid data, when PurchasePolicy is constructed, then id is stored correctly")
    void givenValidData_WhenConstructed_ThenIdIsStoredCorrectly() {
        // Arrange
        int policyId = 10;

        // Act
        PurchasePolicy policy = new PurchasePolicy(policyId, "Policy", 100);

        // Assert
        assertEquals(policyId, policy.getPolicyId());
    }

    @Test
    @DisplayName("Given valid data, when PurchasePolicy is constructed, then description is stored correctly")
    void givenValidData_WhenConstructed_ThenDescriptionIsStoredCorrectly() {
        // Arrange
        String description = "Default purchase policy";

        // Act
        PurchasePolicy policy = new PurchasePolicy(10, description, 100);

        // Assert
        assertEquals(description, policy.getDescription());
    }

    @Test
    @DisplayName("Given valid data, when PurchasePolicy is constructed, then event id is stored correctly")
    void givenValidData_WhenConstructed_ThenEventIdIsStoredCorrectly() {
        // Arrange
        int eventId = 100;

        // Act
        PurchasePolicy policy = new PurchasePolicy(10, "Policy", eventId);

        // Assert
        assertEquals(eventId, policy.getEventId());
    }

    // =========================================================================
    // 2. validatePurchase — default policy
    // =========================================================================

    @Test
    @DisplayName("Given default policy and valid purchase, when validatePurchase is called, then returns true")
    void givenDefaultPolicyAndValidPurchase_WhenValidatePurchaseCalled_ThenReturnsTrue() {
        // Arrange
        int quantity = 1;
        int userAge = 18;
        boolean createsSingleGap = false;

        // Act
        boolean result = purchasePolicy.validatePurchase(quantity, userAge, createsSingleGap);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given default policy and zero quantity, when validatePurchase is called, then returns false")
    void givenDefaultPolicyAndZeroQuantity_WhenValidatePurchaseCalled_ThenReturnsFalse() {
        // Arrange
        int quantity = 0;
        int userAge = 18;
        boolean createsSingleGap = false;

        // Act
        boolean result = purchasePolicy.validatePurchase(quantity, userAge, createsSingleGap);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given default policy and negative quantity, when validatePurchase is called, then returns false")
    void givenDefaultPolicyAndNegativeQuantity_WhenValidatePurchaseCalled_ThenReturnsFalse() {
        // Arrange
        int quantity = -5;
        int userAge = 18;
        boolean createsSingleGap = false;

        // Act
        boolean result = purchasePolicy.validatePurchase(quantity, userAge, createsSingleGap);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given default policy and large quantity, when validatePurchase is called, then returns true")
    void givenDefaultPolicyAndLargeQuantity_WhenValidatePurchaseCalled_ThenReturnsTrue() {
        // Arrange
        int quantity = 1000;
        int userAge = 25;
        boolean createsSingleGap = false;

        // Act
        boolean result = purchasePolicy.validatePurchase(quantity, userAge, createsSingleGap);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given default policy and single seat gap, when validatePurchase is called, then returns true")
    void givenDefaultPolicyAndSingleSeatGap_WhenValidatePurchaseCalled_ThenReturnsTrue() {
        // Arrange
        int quantity = 2;
        int userAge = 25;
        boolean createsSingleGap = true;

        // Act
        boolean result = purchasePolicy.validatePurchase(quantity, userAge, createsSingleGap);

        // Assert
        assertTrue(result);
    }

    // =========================================================================
    // 3. validatePurchase — modified internal constraints
    // =========================================================================

    @Test
    @DisplayName("Given min tickets is 2 and quantity is 1, when validatePurchase is called, then returns false")
    void givenMinTicketsTwoAndQuantityOne_WhenValidatePurchaseCalled_ThenReturnsFalse() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "minTickets", 2);

        // Act
        boolean result = purchasePolicy.validatePurchase(1, 18, false);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given min tickets is 2 and quantity is 2, when validatePurchase is called, then returns true")
    void givenMinTicketsTwoAndQuantityTwo_WhenValidatePurchaseCalled_ThenReturnsTrue() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "minTickets", 2);

        // Act
        boolean result = purchasePolicy.validatePurchase(2, 18, false);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given max tickets is 3 and quantity is 4, when validatePurchase is called, then returns false")
    void givenMaxTicketsThreeAndQuantityFour_WhenValidatePurchaseCalled_ThenReturnsFalse() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "maxTickets", 3);

        // Act
        boolean result = purchasePolicy.validatePurchase(4, 18, false);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given max tickets is 3 and quantity is 3, when validatePurchase is called, then returns true")
    void givenMaxTicketsThreeAndQuantityThree_WhenValidatePurchaseCalled_ThenReturnsTrue() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "maxTickets", 3);

        // Act
        boolean result = purchasePolicy.validatePurchase(3, 18, false);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given min age is 18 and user age is 17, when validatePurchase is called, then returns false")
    void givenMinAgeEighteenAndUserAgeSeventeen_WhenValidatePurchaseCalled_ThenReturnsFalse() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "minAge", 18);

        // Act
        boolean result = purchasePolicy.validatePurchase(1, 17, false);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given min age is 18 and user age is 18, when validatePurchase is called, then returns true")
    void givenMinAgeEighteenAndUserAgeEighteen_WhenValidatePurchaseCalled_ThenReturnsTrue() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "minAge", 18);

        // Act
        boolean result = purchasePolicy.validatePurchase(1, 18, false);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given single seat gap is not allowed and gap is created, when validatePurchase is called, then returns false")
    void givenSingleSeatGapNotAllowedAndGapCreated_WhenValidatePurchaseCalled_ThenReturnsFalse() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "allowSingleSeatGap", false);

        // Act
        boolean result = purchasePolicy.validatePurchase(1, 18, true);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given single seat gap is not allowed and no gap is created, when validatePurchase is called, then returns true")
    void givenSingleSeatGapNotAllowedAndNoGapCreated_WhenValidatePurchaseCalled_ThenReturnsTrue() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "allowSingleSeatGap", false);

        // Act
        boolean result = purchasePolicy.validatePurchase(1, 18, false);

        // Assert
        assertTrue(result);
    }

    // =========================================================================
    // 4. isValid
    // =========================================================================

    @Test
    @DisplayName("Given default policy, when isValid is called, then returns true")
    void givenDefaultPolicy_WhenIsValidCalled_ThenReturnsTrue() {
        // Act
        boolean result = purchasePolicy.isValid();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given min tickets greater than max tickets, when isValid is called, then returns false")
    void givenMinTicketsGreaterThanMaxTickets_WhenIsValidCalled_ThenReturnsFalse() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "minTickets", 5);
        setPrivateField(purchasePolicy, "maxTickets", 2);

        // Act
        boolean result = purchasePolicy.isValid();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given negative min age, when isValid is called, then returns false")
    void givenNegativeMinAge_WhenIsValidCalled_ThenReturnsFalse() throws Exception {
        // Arrange
        setPrivateField(purchasePolicy, "minAge", -1);

        // Act
        boolean result = purchasePolicy.isValid();

        // Assert
        assertFalse(result);
    }

    // =========================================================================
    // 5. Concurrency tests
    // =========================================================================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Given many valid purchase requests, when validatePurchase is called concurrently, then all return true")
    void givenManyValidPurchaseRequests_WhenValidatePurchaseCalledConcurrently_ThenAllReturnTrue() throws Exception {
        // Arrange
        int numberOfThreads = 50;
        int numberOfTasks = 300;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < numberOfTasks; i++) {
            results.add(executor.submit(() -> {
                startSignal.await();
                return purchasePolicy.validatePurchase(1, 21, false);
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

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Given many invalid purchase requests, when validatePurchase is called concurrently, then all return false")
    void givenManyInvalidPurchaseRequests_WhenValidatePurchaseCalledConcurrently_ThenAllReturnFalse() throws Exception {
        // Arrange
        int numberOfThreads = 50;
        int numberOfTasks = 300;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < numberOfTasks; i++) {
            results.add(executor.submit(() -> {
                startSignal.await();
                return purchasePolicy.validatePurchase(0, 21, false);
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

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}