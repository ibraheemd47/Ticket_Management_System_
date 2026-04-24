package com.sdnah.Ticket_Management_System_.Policy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;

@ExtendWith(MockitoExtension.class)
class PolicyServiceAcceptanceTest {

    @Mock
    private IPolicyRepo policyRepo;

    @InjectMocks
    private PolicyService policyService;

    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;
    private SellingPolicy sellingPolicy;

    @BeforeEach
    void setUp() {
        purchasePolicy = new PurchasePolicy(1, "Standard Purchase Policy");
        discountPolicy = new DiscountPolicy(2, "10% Off Discount Policy");
        sellingPolicy = new SellingPolicy(
                3,
                "Regular Selling Policy",
                SellingPolicy.SellingType.REGULAR
        );
    }

    // ==========================================================
    // UC II.2.5: Select Tickets for Event
    // ==========================================================

    @Test
    void checkSelectionPermission_withRegularPolicy_returnsTrue() {
        when(policyRepo.findById(3)).thenReturn(Optional.of(sellingPolicy));

        boolean result = policyService.checkSelectionPermission(3, false);

        assertTrue(result);
        verify(policyRepo).findById(3);
    }

    @Test
    void checkSelectionPermission_withLotteryPolicyForGuest_returnsFalse() {
        SellingPolicy lotteryPolicy =
                new SellingPolicy(4, "Lottery", SellingPolicy.SellingType.LOTTERY);

        when(policyRepo.findById(4)).thenReturn(Optional.of(lotteryPolicy));

        boolean result = policyService.checkSelectionPermission(4, false);

        assertFalse(result);
        verify(policyRepo).findById(4);
    }

    @Test
    void checkSelectionPermission_withLotteryPolicyForMember_returnsTrue() {
        SellingPolicy lotteryPolicy =
                new SellingPolicy(5, "Lottery", SellingPolicy.SellingType.LOTTERY);

        when(policyRepo.findById(5)).thenReturn(Optional.of(lotteryPolicy));

        boolean result = policyService.checkSelectionPermission(5, true);

        assertTrue(result);
        verify(policyRepo).findById(5);
    }

    @Test
    void checkSelectionPermission_withMissingPolicy_returnsTrueByDefault() {
        when(policyRepo.findById(999)).thenReturn(Optional.empty());

        boolean result = policyService.checkSelectionPermission(999, false);

        assertTrue(result);
        verify(policyRepo).findById(999);
    }

    // ==========================================================
    // UC II.2.4: Reserve Tickets in Active Order
    // ==========================================================

    @Test
    void validateReservationRequest_withValidInput_returnsTrue() {
        when(policyRepo.findById(1)).thenReturn(Optional.of(purchasePolicy));

        boolean result = policyService.validateReservationRequest(1, 2, 25);

        assertTrue(result);
        verify(policyRepo).findById(1);
    }

    @Test
    void validateReservationRequest_withInvalidQuantity_returnsFalse() {
        when(policyRepo.findById(1)).thenReturn(Optional.of(purchasePolicy));

        boolean result = policyService.validateReservationRequest(1, 0, 25);

        assertFalse(result);
        verify(policyRepo).findById(1);
    }

    @Test
    void validateReservationRequest_withMissingPolicy_returnsTrueByDefault() {
        when(policyRepo.findById(999)).thenReturn(Optional.empty());

        boolean result = policyService.validateReservationRequest(999, 2, 25);

        assertTrue(result);
        verify(policyRepo).findById(999);
    }

    // ==========================================================
    // UC II.2.8: Checkout Active Order
    // ==========================================================

    @Test
    void processCheckout_withDiscount_returnsDiscountedPrice() {
        discountPolicy.addDiscountRule(
                new DiscountPolicy.PercentageDiscount(10.0)
        );

        when(policyRepo.findById(1)).thenReturn(Optional.of(purchasePolicy));
        when(policyRepo.findById(2)).thenReturn(Optional.of(discountPolicy));

        double result = policyService.processCheckout(
                1,
                2,
                100.0,
                1,
                25,
                ""
        );

        assertEquals(90.0, result, 0.001);
        verify(policyRepo).findById(1);
        verify(policyRepo).findById(2);
    }

    @Test
    void processCheckout_withPurchasePolicyViolation_returnsMinusOne() {
        when(policyRepo.findById(1)).thenReturn(Optional.of(purchasePolicy));

        double result = policyService.processCheckout(
                1,
                2,
                100.0,
                0,
                25,
                ""
        );

        assertEquals(-1.0, result, 0.001);
        verify(policyRepo).findById(1);
        verify(policyRepo, never()).findById(2);
    }

    @Test
    void processCheckout_withMissingPolicies_returnsBasePrice() {
        when(policyRepo.findById(999)).thenReturn(Optional.empty());
        when(policyRepo.findById(888)).thenReturn(Optional.empty());

        double result = policyService.processCheckout(
                999,
                888,
                100.0,
                1,
                25,
                ""
        );

        assertEquals(100.0, result, 0.001);
        verify(policyRepo).findById(999);
        verify(policyRepo).findById(888);
    }

    @Test
    void processCheckout_withValidPurchasePolicyAndMissingDiscountPolicy_returnsBasePrice() {
        when(policyRepo.findById(1)).thenReturn(Optional.of(purchasePolicy));
        when(policyRepo.findById(888)).thenReturn(Optional.empty());

        double result = policyService.processCheckout(
                1,
                888,
                150.0,
                2,
                25,
                ""
        );

        assertEquals(150.0, result, 0.001);
        verify(policyRepo).findById(1);
        verify(policyRepo).findById(888);
    }
}