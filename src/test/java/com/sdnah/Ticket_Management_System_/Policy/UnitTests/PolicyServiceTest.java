package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService — Application Unit Tests")
class PolicyServiceTest {

    @Mock
    private IPolicyRepo policyRepo;

    // =========================================================================
    // 1. checkSelectionPermission
    // =========================================================================

    @Test
    @DisplayName("Given SellingPolicy and member, when checkSelectionPermission is called, then returns true")
    void givenSellingPolicyAndMember_WhenCheckSelectionPermissionCalled_ThenReturnsTrue() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);
        SellingPolicy policy = new SellingPolicy(1, "selling", SellingPolicy.SellingType.REGULAR, 100);

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        // Act
        boolean result = service.checkSelectionPermission(100, true);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given Lottery policy and guest, when checkSelectionPermission is called, then returns false")
    void givenLotteryPolicyAndGuest_WhenCheckSelectionPermissionCalled_ThenReturnsFalse() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);
        SellingPolicy policy = new SellingPolicy(1, "lottery", SellingPolicy.SellingType.LOTTERY, 100);

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        // Act
        boolean result = service.checkSelectionPermission(100, false);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given no SellingPolicy, when checkSelectionPermission is called, then returns true by default")
    void givenNoSellingPolicy_WhenCheckSelectionPermissionCalled_ThenReturnsTrueByDefault() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);

        when(policyRepo.findAll()).thenReturn(List.of());

        // Act
        boolean result = service.checkSelectionPermission(100, false);

        // Assert
        assertTrue(result);
    }

    // =========================================================================
    // 2. validateReservationRequest
    // =========================================================================

    @Test
    @DisplayName("Given PurchasePolicy and valid request, when validateReservationRequest is called, then returns true")
    void givenPurchasePolicyAndValidRequest_WhenValidateReservationRequestCalled_ThenReturnsTrue() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);
        PurchasePolicy policy = new PurchasePolicy(2, "purchase", 100);

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        // Act
        boolean result = service.validateReservationRequest(100, 1, 20);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given PurchasePolicy and invalid request, when validateReservationRequest is called, then returns false")
    void givenPurchasePolicyAndInvalidRequest_WhenValidateReservationRequestCalled_ThenReturnsFalse() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);
        PurchasePolicy policy = new PurchasePolicy(2, "purchase", 100);

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        // Act
        boolean result = service.validateReservationRequest(100, 0, 20);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given no PurchasePolicy, when validateReservationRequest is called, then returns true")
    void givenNoPurchasePolicy_WhenValidateReservationRequestCalled_ThenReturnsTrue() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);

        when(policyRepo.findAll()).thenReturn(List.of());

        // Act
        boolean result = service.validateReservationRequest(100, 0, 20);

        // Assert
        assertTrue(result);
    }

    // =========================================================================
    // 3. calculateCouponDiscount
    // =========================================================================

    @Test
    @DisplayName("Given valid coupon, when calculateCouponDiscount is called, then discount is applied")
    void givenValidCoupon_WhenCalculateCouponDiscountCalled_ThenDiscountIsApplied() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);
        DiscountPolicy policy = new DiscountPolicy(3, "discount", 100);
        policy.addDiscountRule(new DiscountPolicy.CouponDiscount("SAVE20", 20));

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        // Act
        double result = service.calculateCouponDiscount(100, 100.0, 1, "SAVE20");

        // Assert
        assertEquals(80.0, result);
    }

    @Test
    @DisplayName("Given null coupon, when calculateCouponDiscount is called, then original price is returned")
    void givenNullCoupon_WhenCalculateCouponDiscountCalled_ThenOriginalPriceIsReturned() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);

        // Act
        double result = service.calculateCouponDiscount(100, 100.0, 1, null);

        // Assert
        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Given no DiscountPolicy, when calculateCouponDiscount is called, then original price is returned")
    void givenNoDiscountPolicy_WhenCalculateCouponDiscountCalled_ThenOriginalPriceIsReturned() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);

        when(policyRepo.findAll()).thenReturn(List.of());

        // Act
        double result = service.calculateCouponDiscount(100, 100.0, 1, "SAVE20");

        // Assert
        assertEquals(100.0, result);
    }

    // =========================================================================
    // 4. applyGeneralDiscounts
    // =========================================================================

    @Test
    @DisplayName("Given DiscountPolicy, when applyGeneralDiscounts is called, then discount is applied")
    void givenDiscountPolicy_WhenApplyGeneralDiscountsCalled_ThenDiscountIsApplied() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);
        DiscountPolicy policy = new DiscountPolicy(4, "discount", 100);
        policy.addDiscountRule(new DiscountPolicy.PercentageDiscount(10));

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        // Act
        double result = service.applyGeneralDiscounts(100, 100.0, 1);

        // Assert
        assertEquals(90.0, result);
    }

    @Test
    @DisplayName("Given no DiscountPolicy, when applyGeneralDiscounts is called, then original price is returned")
    void givenNoDiscountPolicy_WhenApplyGeneralDiscountsCalled_ThenOriginalPriceIsReturned() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);

        when(policyRepo.findAll()).thenReturn(List.of());

        // Act
        double result = service.applyGeneralDiscounts(100, 100.0, 1);

        // Assert
        assertEquals(100.0, result);
    }

    // =========================================================================
    // 5. isConditionalDiscountSatisfied
    // =========================================================================

    @Test
    @DisplayName("Given conditional discount satisfied, when isConditionalDiscountSatisfied is called, then returns true")
    void givenConditionalDiscountSatisfied_WhenIsConditionalDiscountSatisfiedCalled_ThenReturnsTrue() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);
        DiscountPolicy policy = new DiscountPolicy(5, "discount", 100);
        policy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 20));

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        // Act
        boolean result = service.isConditionalDiscountSatisfied(100, 3);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given conditional discount not satisfied, when isConditionalDiscountSatisfied is called, then returns false")
    void givenConditionalDiscountNotSatisfied_WhenIsConditionalDiscountSatisfiedCalled_ThenReturnsFalse() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);
        DiscountPolicy policy = new DiscountPolicy(5, "discount", 100);
        policy.addDiscountRule(new DiscountPolicy.ConditionalDiscount(3, 20));

        when(policyRepo.findAll()).thenReturn(List.of(policy));

        // Act
        boolean result = service.isConditionalDiscountSatisfied(100, 1);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given no DiscountPolicy, when isConditionalDiscountSatisfied is called, then returns false")
    void givenNoDiscountPolicy_WhenIsConditionalDiscountSatisfiedCalled_ThenReturnsFalse() {
        // Arrange
        PolicyService service = new PolicyService(policyRepo);

        when(policyRepo.findAll()).thenReturn(List.of());

        // Act
        boolean result = service.isConditionalDiscountSatisfied(100, 3);

        // Assert
        assertFalse(result);
    }
}