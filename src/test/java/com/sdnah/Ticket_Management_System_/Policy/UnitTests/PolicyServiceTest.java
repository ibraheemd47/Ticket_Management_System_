package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.PercentageDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.QuantityConditionalDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PolicyService — Application Layer Unit Tests")
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock private PolicyRepository      policyRepo;
    @Mock private CompanyRepository     companyRepo;
    @Mock private IrepresnteUserService representUserService;
    @Mock private Member                actor;
    @Mock private Company               company;

    private PolicyService policyService;

    private static final int  COMPANY_ID = 1;
    private static final UUID EVENT_ID   = UUID.randomUUID();
    private static final String TOKEN    = "token";

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyRepo, companyRepo, representUserService);
    }

    // ── Auth mock helper ──────────────────────────────────────────────────────

    private void mockOwnerAuth() {
        lenient().when(representUserService.requireMember(TOKEN)).thenReturn(actor);
        lenient().when(companyRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        lenient().when(actor.isActive()).thenReturn(true);
        lenient().when(company.isOwner(any())).thenReturn(true);
        lenient().when(company.getCompanyId()).thenReturn(COMPANY_ID);
    }

    // =========================================================================
    // UC II.4.3 — addDiscountRuleToEvent
    // =========================================================================

    @Test
    void GivenDiscountPolicy_WhenAddDiscountRuleToEvent_ThenRuleSaved() {
        DiscountPolicy policy = new DiscountPolicy(1, "Event policy", EVENT_ID, COMPANY_ID);
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.addDiscountRuleToEvent(TOKEN, COMPANY_ID, EVENT_ID,
                new PercentageDiscountRule(10.0, "10% off"));

        verify(policyRepo).savePolicy(policy);
        assertTrue(policy.computeDiscount(new DiscountContext(1, null)) > 0.0);
    }

    @Test
    void GivenNoDiscountPolicy_WhenAddDiscountRuleToEvent_ThenExceptionThrown() {
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.empty());
        mockOwnerAuth();

        assertThrows(IllegalArgumentException.class, () ->
                policyService.addDiscountRuleToEvent(TOKEN, COMPANY_ID, EVENT_ID,
                        new PercentageDiscountRule(10.0, "10% off")));
    }

    // =========================================================================
    // UC II.4.3 — addDiscountRuleToCompany
    // =========================================================================

    @Test
    void GivenCompanyDiscountPolicy_WhenAddDiscountRuleToCompany_ThenRuleSaved() {
        DiscountPolicy policy = new DiscountPolicy(2, "Company policy", null, COMPANY_ID);
        when(policyRepo.findDiscountPolicyByCompanyIdAndEventIdIsNull(COMPANY_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.addDiscountRuleToCompany(TOKEN, COMPANY_ID,
                new PercentageDiscountRule(15.0, "15% off"));

        verify(policyRepo).savePolicy(policy);
    }

    // =========================================================================
    // UC II.4.3 — setDiscountRulesForEvent
    // =========================================================================

    @Test
    void GivenDiscountPolicy_WhenSetDiscountRulesForEvent_ThenRulesReplaced() {
        DiscountPolicy policy = new DiscountPolicy(3, "Event policy", EVENT_ID, COMPANY_ID);
        policy.addRule(new PercentageDiscountRule(5.0, "5% off"));
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.setDiscountRulesForEvent(TOKEN, COMPANY_ID, EVENT_ID,
                List.of(new PercentageDiscountRule(30.0, "30% off")), false);

        verify(policyRepo).savePolicy(policy);
        assertEquals(30.0, policy.computeDiscount(new DiscountContext(1, null)), 0.001);
    }

    // =========================================================================
    // UC II.4.3 — setDiscountRulesForCompany
    // =========================================================================

    @Test
    void GivenCompanyDiscountPolicy_WhenSetDiscountRulesForCompany_ThenRulesReplaced() {
        DiscountPolicy policy = new DiscountPolicy(4, "Company policy", null, COMPANY_ID);
        when(policyRepo.findDiscountPolicyByCompanyIdAndEventIdIsNull(COMPANY_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.setDiscountRulesForCompany(TOKEN, COMPANY_ID,
                List.of(new PercentageDiscountRule(20.0, "20% off")), false);

        verify(policyRepo).savePolicy(policy);
        assertEquals(20.0, policy.computeDiscount(new DiscountContext(1, null)), 0.001);
    }

    // =========================================================================
    // UC II.4.3 — addPurchaseRuleToEvent
    // =========================================================================

    @Test
    void GivenPurchasePolicy_WhenAddPurchaseRuleToEvent_ThenRuleSaved() {
        PurchasePolicy policy = new PurchasePolicy(5, "Event policy", EVENT_ID, COMPANY_ID);
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.addPurchaseRuleToEvent(TOKEN, COMPANY_ID, EVENT_ID, new MinAgeRule(18));

        verify(policyRepo).savePolicy(policy);
        assertFalse(policy.validatePurchase(1, 16, false));
        assertTrue(policy.validatePurchase(1, 18, false));
    }

    @Test
    void GivenNoPurchasePolicy_WhenAddPurchaseRuleToEvent_ThenExceptionThrown() {
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.empty());
        mockOwnerAuth();

        assertThrows(IllegalArgumentException.class, () ->
                policyService.addPurchaseRuleToEvent(TOKEN, COMPANY_ID, EVENT_ID,
                        new MinAgeRule(18)));
    }

    // =========================================================================
    // UC II.4.3 — addPurchaseRuleToCompany
    // =========================================================================

    @Test
    void GivenCompanyPurchasePolicy_WhenAddPurchaseRuleToCompany_ThenRuleSaved() {
        PurchasePolicy policy = new PurchasePolicy(6, "Company policy", null, COMPANY_ID);
        when(policyRepo.findPurchasePolicyByCompanyIdAndEventIdIsNull(COMPANY_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.addPurchaseRuleToCompany(TOKEN, COMPANY_ID, new MinAgeRule(18));

        verify(policyRepo).savePolicy(policy);
    }

    // =========================================================================
    // UC II.4.3 — setPurchaseRulesForEvent
    // =========================================================================

    @Test
    void GivenPurchasePolicy_WhenSetPurchaseRulesForEvent_ThenRulesReplaced() {
        PurchasePolicy policy = new PurchasePolicy(7, "Event policy", EVENT_ID, COMPANY_ID);
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.setPurchaseRulesForEvent(TOKEN, COMPANY_ID, EVENT_ID,
                List.of(new MaxTicketsRule(5)), PurchasePolicy.Operator.AND);

        verify(policyRepo).savePolicy(policy);
        assertFalse(policy.validatePurchase(6, 20, false));
        assertTrue(policy.validatePurchase(3, 20, false));
    }

    @Test
    void GivenPurchasePolicy_WhenSetPurchaseRulesOrForEvent_ThenOrLogicApplied() {
        PurchasePolicy policy = new PurchasePolicy(8, "OR policy", EVENT_ID, COMPANY_ID);
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.setPurchaseRulesForEvent(TOKEN, COMPANY_ID, EVENT_ID,
                List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);

        verify(policyRepo).savePolicy(policy);
        assertTrue(policy.validatePurchase(1, 20, false));   // passes MaxTickets
        assertTrue(policy.validatePurchase(100, 20, false)); // passes MinTickets
        assertFalse(policy.validatePurchase(5, 20, false));  // fails both
    }

    // =========================================================================
    // Authorization — non-owner cannot modify
    // =========================================================================

    @Test
    void GivenNonOwner_WhenAddDiscountRule_ThenThrowException() {
        DiscountPolicy policy = new DiscountPolicy(9, "Policy", EVENT_ID, COMPANY_ID);
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));
        when(representUserService.requireMember(TOKEN)).thenReturn(actor);
        when(companyRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(actor.isActive()).thenReturn(true);
        when(company.isOwner(any())).thenReturn(false);
        when(company.getCompanyId()).thenReturn(COMPANY_ID);

        assertThrows(RuntimeException.class, () ->
                policyService.addDiscountRuleToEvent(TOKEN, COMPANY_ID, EVENT_ID,
                        new PercentageDiscountRule(10.0, "10% off")));
    }
}