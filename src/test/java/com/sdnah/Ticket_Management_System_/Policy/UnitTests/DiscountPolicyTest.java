package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;

@DisplayName("DiscountPolicy — Domain Unit Tests")
class DiscountPolicyTest {

    private DiscountPolicy discountPolicy;

    @BeforeEach
    void setUp() {
        discountPolicy = new DiscountPolicy(20, "Default discount policy", 2000);
    }

    // =========================================================================
    // 1. Constructor + general behavior
    // =========================================================================

    @Test
    @DisplayName("Given valid data, when DiscountPolicy is constructed, then fields are stored correctly")
    void givenValidData_WhenConstructed_ThenFieldsAreStoredCorrectly() {
        // Arrange
        int policyId = 20;
        String description = "Discount policy";
        int eventId = 200;

        // Act
        DiscountPolicy policy = new DiscountPolicy(policyId, description, eventId);

        // Assert
        assertEquals(policyId, policy.getPolicyId());
        assertEquals(description, policy.getDescription());
        assertEquals(eventId, policy.getEventId());
    }

    @Test
    @DisplayName("Given default discount policy, when isValid is called, then returns true")
    void givenDefaultDiscountPolicy_WhenIsValidCalled_ThenReturnsTrue() {
        // Act
        boolean result = discountPolicy.isValid();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given no discounts, when hasDiscounts is called, then returns false")
    void givenNoDiscounts_WhenHasDiscountsCalled_ThenReturnsFalse() {
        // Act
        boolean result = discountPolicy.hasDiscounts();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given discount rule, when hasDiscounts is called, then returns true")
    void givenDiscountRule_WhenHasDiscountsCalled_ThenReturnsTrue() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        // Act
        boolean result = discountPolicy.hasDiscounts();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given null discount rule, when addDiscountRule is called, then rule is ignored")
    void givenNullDiscountRule_WhenAddDiscountRuleCalled_ThenRuleIsIgnored() {
        // Act
        discountPolicy.addDiscountRule(null);

        // Assert
        assertFalse(discountPolicy.hasDiscounts());
    }

    // =========================================================================
    // 2. calculateFinalPrice — basic and negative cases
    // =========================================================================

    @Test
    @DisplayName("Given no discounts, when calculateFinalPrice is called, then original price is returned")
    void givenNoDiscounts_WhenCalculateFinalPriceCalled_ThenOriginalPriceIsReturned() {
        // Act
        double result = discountPolicy.calculateFinalPrice(100.0, 2, null);

        // Assert
        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Given negative subtotal, when calculateFinalPrice is called, then throws IllegalArgumentException")
    void givenNegativeSubtotal_WhenCalculateFinalPriceCalled_ThenThrowsIllegalArgumentException() {
        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> discountPolicy.calculateFinalPrice(-10.0, 1, null)
        );
    }

    // =========================================================================
    // 3. PercentageDiscount
    // =========================================================================

    @Test
    @DisplayName("Given 10 percent discount, when calculateFinalPrice is called, then returns discounted price")
    void givenTenPercentDiscount_WhenCalculateFinalPriceCalled_ThenReturnsDiscountedPrice() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        // Act
        double result = discountPolicy.calculateFinalPrice(100.0, 1, null);

        // Assert
        assertEquals(90.0, result);
    }

    @Test
    @DisplayName("Given multiple percentage discounts, when calculateFinalPrice is called, then best discount is selected")
    void givenMultiplePercentageDiscounts_WhenCalculateFinalPriceCalled_ThenBestDiscountIsSelected() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(30));

        // Act
        double result = discountPolicy.calculateFinalPrice(100.0, 1, null);

        // Assert
        assertEquals(70.0, result);
    }

    // =========================================================================
    // 4. ConditionalDiscount
    // =========================================================================

    @Test
    @DisplayName("Given conditional discount and condition is met, when calculateFinalPrice is called, then discount is applied")
    void givenConditionalDiscountAndConditionMet_WhenCalculateFinalPriceCalled_ThenDiscountIsApplied() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 25));

        // Act
        double result = discountPolicy.calculateFinalPrice(100.0, 3, null);

        // Assert
        assertEquals(75.0, result);
    }

    @Test
    @DisplayName("Given conditional discount and condition is not met, when calculateFinalPrice is called, then original price is returned")
    void givenConditionalDiscountAndConditionNotMet_WhenCalculateFinalPriceCalled_ThenOriginalPriceIsReturned() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 25));

        // Act
        double result = discountPolicy.calculateFinalPrice(100.0, 2, null);

        // Assert
        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Given conditional discount and quantity is enough, when isAnyConditionalDiscountSatisfied is called, then returns true")
    void givenConditionalDiscountAndQuantityEnough_WhenIsAnyConditionalDiscountSatisfiedCalled_ThenReturnsTrue() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 25));

        // Act
        boolean result = discountPolicy.isAnyConditionalDiscountSatisfied(3);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given only percentage discount, when isAnyConditionalDiscountSatisfied is called, then returns false")
    void givenOnlyPercentageDiscount_WhenIsAnyConditionalDiscountSatisfiedCalled_ThenReturnsFalse() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        // Act
        boolean result = discountPolicy.isAnyConditionalDiscountSatisfied(10);

        // Assert
        assertFalse(result);
    }

    // =========================================================================
    // 5. CouponDiscount
    // =========================================================================

    @Test
    @DisplayName("Given valid coupon, when calculateFinalPrice is called, then coupon discount is applied")
    void givenValidCoupon_WhenCalculateFinalPriceCalled_ThenCouponDiscountIsApplied() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));

        // Act
        double result = discountPolicy.calculateFinalPrice(100.0, 1, "SAVE20");

        // Assert
        assertEquals(80.0, result);
    }

    @Test
    @DisplayName("Given invalid coupon, when calculateFinalPrice is called, then original price is returned")
    void givenInvalidCoupon_WhenCalculateFinalPriceCalled_ThenOriginalPriceIsReturned() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));

        // Act
        double result = discountPolicy.calculateFinalPrice(100.0, 1, "BAD");

        // Assert
        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Given coupon discount greater than price, when calculateFinalPrice is called, then returns zero")
    void givenCouponDiscountGreaterThanPrice_WhenCalculateFinalPriceCalled_ThenReturnsZero() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("FREE", 500));

        // Act
        double result = discountPolicy.calculateFinalPrice(100.0, 1, "FREE");

        // Assert
        assertEquals(0.0, result);
    }

    // =========================================================================
    // 6. Mixed discounts + toString
    // =========================================================================

    @Test
    @DisplayName("Given coupon and percentage discounts, when coupon gives better price, then coupon is selected")
    void givenCouponAndPercentageDiscounts_WhenCouponBetter_ThenCouponIsSelected() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE40", 40));

        // Act
        double result = discountPolicy.calculateFinalPrice(100.0, 1, "SAVE40");

        // Assert
        assertEquals(60.0, result);
    }

    @Test
    @DisplayName("Given discount policy with rule, when toString is called, then contains policy data")
    void givenDiscountPolicyWithRule_WhenToStringCalled_ThenContainsPolicyData() {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        // Act
        String result = discountPolicy.toString();

        // Assert
        assertTrue(result.contains("DiscountPolicy"));
        assertTrue(result.contains("id=20"));
        assertTrue(result.contains("activeRulesCount=1"));
    }

    // =========================================================================
    // 7. Concurrency tests
    // =========================================================================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Given many checkout calculations, when calculateFinalPrice is called concurrently, then all return same price")
    void givenManyCheckoutCalculations_WhenCalculateFinalPriceCalledConcurrently_ThenAllReturnSamePrice() throws Exception {
        // Arrange
        discountPolicy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));
        discountPolicy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 25));
        discountPolicy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE40", 40));

        int numberOfThreads = 50;
        int numberOfTasks = 300;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Double>> results = new ArrayList<>();

        for (int i = 0; i < numberOfTasks; i++) {
            results.add(executor.submit(() -> {
                startSignal.await();
                return discountPolicy.calculateFinalPrice(100.0, 3, "SAVE40");
            }));
        }

        // Act
        startSignal.countDown();

        // Assert
        for (Future<Double> result : results) {
            assertEquals(60.0, result.get());
        }

        executor.shutdownNow();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Given many invalid checkout calculations, when calculateFinalPrice is called concurrently, then all throw exception safely")
    void givenManyInvalidCheckoutCalculations_WhenCalculateFinalPriceCalledConcurrently_ThenAllThrowExceptionSafely() throws Exception {
        // Arrange
        int numberOfThreads = 30;
        int numberOfTasks = 200;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < numberOfTasks; i++) {
            results.add(executor.submit(() -> {
                startSignal.await();

                try {
                    discountPolicy.calculateFinalPrice(-100.0, 1, null);
                    return false;
                } catch (IllegalArgumentException e) {
                    return true;
                }
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