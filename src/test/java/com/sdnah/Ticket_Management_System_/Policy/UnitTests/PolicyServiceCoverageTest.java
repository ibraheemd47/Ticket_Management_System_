package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

/**
 * Coverage-focused tests for PolicyService — fills in the
 * {@code setPurchaseRulesForCompany} branch and authentication failure modes
 * not covered by the main PolicyServiceTest.
 */
@DisplayName("PolicyService — Coverage Tests")
@ExtendWith(MockitoExtension.class)
class PolicyServiceCoverageTest {

    @Mock private PolicyRepository      policyRepo;
    @Mock private CompanyRepository     companyRepo;
    @Mock private IrepresnteUserService representUserService;
    @Mock private Member                actor;
    @Mock private Company               company;

    private PolicyService policyService;

    private static final UUID COMPANY_ID = UUID.randomUUID();
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

    @Test
    @DisplayName("setPurchaseRulesForCompany loads company policy, applies rules, and saves")
    void setPurchaseRulesForCompany_success() {
        mockOwnerAuth();
        PurchasePolicy policy = new PurchasePolicy(2, "Company policy", null, COMPANY_ID);
        when(policyRepo.findPurchasePolicyByCompanyIdAndEventIdIsNull(COMPANY_ID))
                .thenReturn(Optional.of(policy));
        when(policyRepo.savePolicy(policy)).thenReturn(policy);

        List<PurchaseRule> rules = List.of(new MinAgeRule(18));

        policyService.setPurchaseRulesForCompany(TOKEN, COMPANY_ID, rules,
                PurchasePolicy.Operator.AND);

        verify(policyRepo).savePolicy(policy);
    }

    @Test
    @DisplayName("setPurchaseRulesForCompany throws when no company policy exists")
    void setPurchaseRulesForCompany_noPolicy() {
        mockOwnerAuth();
        when(policyRepo.findPurchasePolicyByCompanyIdAndEventIdIsNull(COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> policyService.setPurchaseRulesForCompany(TOKEN, COMPANY_ID,
                        List.of(new MinAgeRule(18)), PurchasePolicy.Operator.AND));
        verify(policyRepo, never()).savePolicy(any());
    }

    @Test
    @DisplayName("setPurchaseRulesForCompany rejects blank token")
    void setPurchaseRulesForCompany_blankToken() {
        assertThrows(SecurityException.class,
                () -> policyService.setPurchaseRulesForCompany("  ", COMPANY_ID,
                        List.of(new MinAgeRule(18)), PurchasePolicy.Operator.AND));
    }

    @Test
    @DisplayName("setPurchaseRulesForCompany rejects null token")
    void setPurchaseRulesForCompany_nullToken() {
        assertThrows(SecurityException.class,
                () -> policyService.setPurchaseRulesForCompany(null, COMPANY_ID,
                        List.of(new MinAgeRule(18)), PurchasePolicy.Operator.AND));
    }

    @Test
    @DisplayName("setPurchaseRulesForCompany throws when company id is unknown")
    void setPurchaseRulesForCompany_companyNotFound() {
        when(representUserService.requireMember(TOKEN)).thenReturn(actor);
        when(companyRepo.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> policyService.setPurchaseRulesForCompany(TOKEN, COMPANY_ID,
                        List.of(new MinAgeRule(18)), PurchasePolicy.Operator.AND));
    }
}
