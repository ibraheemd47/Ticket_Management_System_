package com.sdnah.Ticket_Management_System_.Policy.IntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

@SpringBootTest
@ActiveProfiles("test")
class PolicyServiceIntegrationTest {

    private static final int COMPANY_ID = 1;
    private static final String TOKEN   = "test-token";

    @Autowired private PolicyService    policyService;
    @Autowired private PolicyRepository policyRepository;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
        eventId = UUID.randomUUID();
    }

    // =========================================================================
    // UC II.4.3 — addDiscountRuleToEvent (persisted)
    // =========================================================================

    @Test
    @DisplayName("Given discount policy, when addDiscountRuleToEvent, then discount applied")
    void givenDiscountPolicy_WhenAddDiscountRuleToEvent_ThenDiscountApplied() {
        policyRepository.saveAndFlush(
                new DiscountPolicy(1, "Event policy", eventId, COMPANY_ID));

        policyService.addDiscountRuleToEvent(TOKEN, COMPANY_ID, eventId,
                new PercentageDiscountRule(20.0, "20% off"));

        DiscountPolicy loaded = policyRepository
                .findDiscountPolicyByEventId(eventId).orElseThrow();
        assertTrue(loaded.computeDiscount(
                new com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount
                        .DiscountContext(1, null)) > 0.0);
    }

    @Test
    @DisplayName("Given no discount policy, when addDiscountRuleToEvent, then exception thrown")
    void givenNoDiscountPolicy_WhenAddDiscountRuleToEvent_ThenExceptionThrown() {
        assertThrows(Exception.class, () ->
                policyService.addDiscountRuleToEvent(TOKEN, COMPANY_ID, eventId,
                        new PercentageDiscountRule(10.0, "10% off")));
    }

    // =========================================================================
    // UC II.4.3 — addDiscountRuleToCompany (persisted)
    // =========================================================================

    @Test
    @DisplayName("Given company discount policy, when addDiscountRuleToCompany, then discount applied")
    void givenCompanyDiscountPolicy_WhenAddDiscountRuleToCompany_ThenDiscountApplied() {
        policyRepository.saveAndFlush(
                new DiscountPolicy(2, "Company policy", null, COMPANY_ID));

        policyService.addDiscountRuleToCompany(TOKEN, COMPANY_ID,
                new PercentageDiscountRule(10.0, "10% off"));

        DiscountPolicy loaded = policyRepository
                .findDiscountPolicyByCompanyIdAndEventIdIsNull(COMPANY_ID).orElseThrow();
        assertEquals(10.0, loaded.computeDiscount(
                new com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount
                        .DiscountContext(1, null)), 0.001);
    }

    // =========================================================================
    // UC II.4.3 — setDiscountRulesForEvent (persisted)
    // =========================================================================

    @Test
    @DisplayName("Given discount policy, when setDiscountRulesForEvent, then rules replaced")
    void givenDiscountPolicy_WhenSetDiscountRulesForEvent_ThenRulesReplaced() {
        DiscountPolicy policy = new DiscountPolicy(3, "Event policy", eventId, COMPANY_ID);
        policy.addRule(new PercentageDiscountRule(5.0, "5% off"));
        policyRepository.saveAndFlush(policy);

        policyService.setDiscountRulesForEvent(TOKEN, COMPANY_ID, eventId,
                List.of(new PercentageDiscountRule(30.0, "30% off")), false);

        DiscountPolicy loaded = policyRepository
                .findDiscountPolicyByEventId(eventId).orElseThrow();
        assertEquals(30.0, loaded.computeDiscount(
                new com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount
                        .DiscountContext(1, null)), 0.001);
    }

    // =========================================================================
    // UC II.4.3 — addPurchaseRuleToEvent (persisted)
    // =========================================================================

    @Test
    @DisplayName("Given purchase policy, when addPurchaseRuleToEvent, then rule validated")
    void givenPurchasePolicy_WhenAddPurchaseRuleToEvent_ThenRuleValidated() {
        policyRepository.saveAndFlush(
                new PurchasePolicy(4, "Event policy", eventId, COMPANY_ID));

        policyService.addPurchaseRuleToEvent(TOKEN, COMPANY_ID, eventId, new MinAgeRule(18));

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByEventId(eventId).orElseThrow();
        assertFalse(loaded.validatePurchase(1, 16, false));
        assertTrue(loaded.validatePurchase(1, 18, false));
    }

    // =========================================================================
    // UC II.4.3 — addPurchaseRuleToCompany (persisted)
    // =========================================================================

    @Test
    @DisplayName("Given company purchase policy, when addPurchaseRuleToCompany, then rule applied")
    void givenCompanyPurchasePolicy_WhenAddPurchaseRuleToCompany_ThenRuleApplied() {
        policyRepository.saveAndFlush(
                new PurchasePolicy(5, "Company policy", null, COMPANY_ID));

        policyService.addPurchaseRuleToCompany(TOKEN, COMPANY_ID, new MaxTicketsRule(5));

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByCompanyIdAndEventIdIsNull(COMPANY_ID).orElseThrow();
        assertFalse(loaded.validatePurchase(6, 20, false));
        assertTrue(loaded.validatePurchase(3, 20, false));
    }

    // =========================================================================
    // UC II.4.3 — setPurchaseRulesForEvent AND/OR (persisted)
    // =========================================================================

    @Test
    @DisplayName("Given purchase policy, when setPurchaseRulesForEvent AND, then all must pass")
    void givenPurchasePolicy_WhenSetPurchaseRulesAnd_ThenAllMustPass() {
        policyRepository.saveAndFlush(
                new PurchasePolicy(6, "Event policy", eventId, COMPANY_ID));

        policyService.setPurchaseRulesForEvent(TOKEN, COMPANY_ID, eventId,
                List.of(new MinAgeRule(18), new MaxTicketsRule(5)),
                PurchasePolicy.Operator.AND);

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByEventId(eventId).orElseThrow();
        assertTrue(loaded.validatePurchase(3, 20, false));
        assertFalse(loaded.validatePurchase(6, 20, false));
        assertFalse(loaded.validatePurchase(1, 16, false));
    }

    @Test
    @DisplayName("Given purchase policy, when setPurchaseRulesForEvent OR, then one must pass")
    void givenPurchasePolicy_WhenSetPurchaseRulesOr_ThenOneMustPass() {
        policyRepository.saveAndFlush(
                new PurchasePolicy(7, "OR policy", eventId, COMPANY_ID));

        policyService.setPurchaseRulesForEvent(TOKEN, COMPANY_ID, eventId,
                List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);

        PurchasePolicy loaded = policyRepository
                .findPurchasePolicyByEventId(eventId).orElseThrow();
        assertTrue(loaded.validatePurchase(1, 20, false));
        assertTrue(loaded.validatePurchase(100, 20, false));
        assertFalse(loaded.validatePurchase(5, 20, false));
    }

    // =========================================================================
    // Selling Policy — still created and queried directly
    // =========================================================================

    @Test
    @DisplayName("Given regular selling policy, when saved, then isSelectionAllowed for guest")
    void givenRegularSellingPolicy_WhenSaved_ThenGuestAllowed() {
        policyRepository.saveAndFlush(new SellingPolicy(
                8, "Regular", SellingPolicy.SellingType.REGULAR, eventId, COMPANY_ID));

        SellingPolicy loaded = policyRepository
                .findSellingPolicyByEventId(eventId).orElseThrow();
        assertTrue(loaded.isSelectionAllowed(false));
    }

    @Test
    @DisplayName("Given lottery selling policy, when saved, then guest rejected")
    void givenLotterySellingPolicy_WhenSaved_ThenGuestRejected() {
        policyRepository.saveAndFlush(new SellingPolicy(
                9, "Lottery", SellingPolicy.SellingType.LOTTERY, eventId, COMPANY_ID));

        SellingPolicy loaded = policyRepository
                .findSellingPolicyByEventId(eventId).orElseThrow();
        assertFalse(loaded.isSelectionAllowed(false));
        assertTrue(loaded.isSelectionAllowed(true));
    }
}