package com.sdnah.Ticket_Management_System_.Policy.AcceptanceTests;

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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PolicyService — Acceptance Tests")
@ExtendWith(MockitoExtension.class)
class PolicyServiceAcceptanceTest {

    @Mock private PolicyRepository      policyRepo;
    @Mock private CompanyRepository     companyRepo;
    @Mock private IrepresnteUserService representUserService;
    @Mock private Member                actor;
    @Mock private Company               company;

    private PolicyService policyService;

    private static final UUID  COMPANY_ID = UUID.randomUUID();
    private static final UUID EVENT_ID   = UUID.randomUUID();
    private static final String TOKEN    = "token";

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyRepo, companyRepo, representUserService);
    }

    private void mockOwnerAuth() {
        lenient().when(representUserService.requireMember(TOKEN)).thenReturn(actor);
        lenient().when(companyRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        lenient().when(actor.isActive()).thenReturn(true);
        lenient().when(company.isOwner(any())).thenReturn(true);
        lenient().when(company.getCompanyId()).thenReturn(COMPANY_ID);
    }

    // =========================================================================
    // UC II.4.3 — Discount Rules — Event
    // =========================================================================

    @Test
    @DisplayName("GivenDiscountPolicy_WhenAddDiscountRuleToEvent_ThenRuleSaved")
    void GivenDiscountPolicy_WhenAddDiscountRuleToEvent_ThenRuleSaved() {
        DiscountPolicy policy = new DiscountPolicy(1, "Event policy", EVENT_ID, COMPANY_ID);
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.addDiscountRuleToEvent(TOKEN, COMPANY_ID, EVENT_ID,
                new PercentageDiscountRule(20.0, "20% off"));

        verify(policyRepo).savePolicy(policy);
        assertEquals(20.0, policy.computeDiscount(new DiscountContext(1, null)), 0.001);
    }

    @Test
    @DisplayName("GivenNoDiscountPolicy_WhenAddDiscountRuleToEvent_ThenExceptionThrown")
    void GivenNoDiscountPolicy_WhenAddDiscountRuleToEvent_ThenExceptionThrown() {
        when(policyRepo.findDiscountPolicyByEventId(EVENT_ID)).thenReturn(Optional.empty());
        mockOwnerAuth();

        assertThrows(IllegalArgumentException.class, () ->
                policyService.addDiscountRuleToEvent(TOKEN, COMPANY_ID, EVENT_ID,
                        new PercentageDiscountRule(10.0, "10% off")));
    }

    @Test
    @DisplayName("GivenDiscountPolicy_WhenSetDiscountRulesForEvent_ThenRulesReplacedAndSaved")
    void GivenDiscountPolicy_WhenSetDiscountRulesForEvent_ThenRulesReplacedAndSaved() {
        DiscountPolicy policy = new DiscountPolicy(2, "Event policy", EVENT_ID, COMPANY_ID);
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
    // UC II.4.3 — Discount Rules — Company
    // =========================================================================

    @Test
    @DisplayName("GivenCompanyDiscountPolicy_WhenAddDiscountRuleToCompany_ThenRuleSaved")
    void GivenCompanyDiscountPolicy_WhenAddDiscountRuleToCompany_ThenRuleSaved() {
        DiscountPolicy policy = new DiscountPolicy(3, "Company policy", null, COMPANY_ID);
        when(policyRepo.findDiscountPolicyByCompanyIdAndEventIdIsNull(COMPANY_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.addDiscountRuleToCompany(TOKEN, COMPANY_ID,
                new CouponDiscountRule(15.0, "SAVE15"));

        verify(policyRepo).savePolicy(policy);
    }

    @Test
    @DisplayName("GivenCompanyDiscountPolicy_WhenSetDiscountRulesForCompany_ThenRulesReplaced")
    void GivenCompanyDiscountPolicy_WhenSetDiscountRulesForCompany_ThenRulesReplaced() {
        DiscountPolicy policy = new DiscountPolicy(4, "Company policy", null, COMPANY_ID);
        when(policyRepo.findDiscountPolicyByCompanyIdAndEventIdIsNull(COMPANY_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.setDiscountRulesForCompany(TOKEN, COMPANY_ID,
                List.of(new QuantityConditionalDiscountRule(3, 10.0)), false);

        verify(policyRepo).savePolicy(policy);
        assertEquals(10.0, policy.computeDiscount(new DiscountContext(3, null)), 0.001);
        assertEquals(0.0,  policy.computeDiscount(new DiscountContext(2, null)), 0.001);
    }

    // =========================================================================
    // UC II.4.3 — Purchase Rules — Event
    // =========================================================================

    @Test
    @DisplayName("GivenPurchasePolicy_WhenAddPurchaseRuleToEvent_ThenRuleSaved")
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
    @DisplayName("GivenNoPurchasePolicy_WhenAddPurchaseRuleToEvent_ThenExceptionThrown")
    void GivenNoPurchasePolicy_WhenAddPurchaseRuleToEvent_ThenExceptionThrown() {
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.empty());
        mockOwnerAuth();

        assertThrows(IllegalArgumentException.class, () ->
                policyService.addPurchaseRuleToEvent(TOKEN, COMPANY_ID, EVENT_ID,
                        new MinAgeRule(18)));
    }

    @Test
    @DisplayName("GivenPurchasePolicy_WhenSetPurchaseRulesForEvent_AndOperator_ThenAllMustPass")
    void GivenPurchasePolicy_WhenSetPurchaseRulesForEventAnd_ThenAllMustPass() {
        PurchasePolicy policy = new PurchasePolicy(6, "AND policy", EVENT_ID, COMPANY_ID);
        when(policyRepo.findPurchasePolicyByEventId(EVENT_ID)).thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.setPurchaseRulesForEvent(TOKEN, COMPANY_ID, EVENT_ID,
                List.of(new MinAgeRule(18), new MaxTicketsRule(5)),
                PurchasePolicy.Operator.AND);

        verify(policyRepo).savePolicy(policy);
        assertTrue(policy.validatePurchase(3, 20, false));  // both pass
        assertFalse(policy.validatePurchase(6, 20, false)); // max tickets fails
        assertFalse(policy.validatePurchase(1, 16, false)); // age fails
    }

    @Test
    @DisplayName("GivenPurchasePolicy_WhenSetPurchaseRulesForEvent_OrOperator_ThenOneMustPass")
    void GivenPurchasePolicy_WhenSetPurchaseRulesForEventOr_ThenOneMustPass() {
        PurchasePolicy policy = new PurchasePolicy(7, "OR policy", EVENT_ID, COMPANY_ID);
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
    // UC II.4.3 — Purchase Rules — Company
    // =========================================================================

    @Test
    @DisplayName("GivenCompanyPurchasePolicy_WhenAddPurchaseRuleToCompany_ThenRuleSaved")
    void GivenCompanyPurchasePolicy_WhenAddPurchaseRuleToCompany_ThenRuleSaved() {
        PurchasePolicy policy = new PurchasePolicy(8, "Company policy", null, COMPANY_ID);
        when(policyRepo.findPurchasePolicyByCompanyIdAndEventIdIsNull(COMPANY_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);
        mockOwnerAuth();

        policyService.addPurchaseRuleToCompany(TOKEN, COMPANY_ID, new MaxTicketsRule(10));

        verify(policyRepo).savePolicy(policy);
        assertFalse(policy.validatePurchase(11, 20, false));
        assertTrue(policy.validatePurchase(5, 20, false));
    }

    // =========================================================================
    // Authorization — non-owner rejected
    // =========================================================================

    @Test
    @DisplayName("GivenNonOwner_WhenAddDiscountRule_ThenThrowException")
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

    // =========================================================================
    // Concurrency — policy reads are thread-safe
    // =========================================================================

    @Test
    @Timeout(5)
    @DisplayName("GivenManyConcurrentRequests_WhenComputingDiscount_ThenAllResultsConsistent")
    void GivenManyConcurrentRequests_WhenComputingDiscount_ThenAllResultsConsistent()
            throws Exception {
        DiscountPolicy policy = new DiscountPolicy(10, "Concurrent discount", EVENT_ID, COMPANY_ID);
        policy.addRule(new PercentageDiscountRule(25.0, "25% off"));

        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        ConcurrentLinkedQueue<Double> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                start.await();
                results.add(policy.computeFinalPrice(200.0, new DiscountContext(2, null)));
                done.countDown();
                return null;
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        assertEquals(threads, results.size());
        assertTrue(results.stream().allMatch(r -> Math.abs(r - 150.0) < 0.001));
    }
}