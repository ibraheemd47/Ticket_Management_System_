package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.PolicyAuthorizationDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;

@Service
public class PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    private final PolicyRepository                policyRepo;
    private final CompanyRepository               companyRepo;
    private final IrepresnteUserService           representUserService;
    private final PolicyAuthorizationDomainService policyAuthService;

    @Autowired
    public PolicyService(PolicyRepository policyRepo,
                         CompanyRepository companyRepo,
                         IrepresnteUserService representUserService) {
        this.policyRepo          = policyRepo;
        this.companyRepo         = companyRepo;
        this.representUserService = representUserService;
        this.policyAuthService   = new PolicyAuthorizationDomainService();
    }

    

    // =========================================================================
    // UC II.4.3 — Add discount rule to EVENT policy
    // =========================================================================
    public void addDiscountRuleToEvent(String actorToken, int companyId,
                                       UUID eventId, DiscountRule newRule) {
        logger.info("Add discount rule to event, companyId={}, eventId={}", companyId, eventId);

        Member  actor   = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);

        DiscountPolicy policy = findDiscountPolicyByEventId(eventId).orElseThrow(() -> {
            logger.error("No discount policy found for eventId={}", eventId);
            return new IllegalArgumentException("No discount policy found for eventId=" + eventId);
        });

        policyAuthService.assertCanModifyDiscountPolicy(actor, company, policy);
        policy.addRule(newRule);
        policyRepo.savePolicy(policy);
        logger.info("Discount rule added to event, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Add discount rule to COMPANY policy (no eventId needed)
    // =========================================================================
    public void addDiscountRuleToCompany(String actorToken, int companyId, DiscountRule newRule) {
        logger.info("Add discount rule to company, companyId={}", companyId);

        Member  actor   = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);

        DiscountPolicy policy = findDiscountPolicyByCompanyId(companyId).orElseThrow(() -> {
            logger.error("No company discount policy found for companyId={}", companyId);
            return new IllegalArgumentException("No company discount policy found for companyId=" + companyId);
        });

        policyAuthService.assertCanModifyDiscountPolicy(actor, company, policy);
        policy.addRule(newRule);
        policyRepo.savePolicy(policy);
        logger.info("Discount rule added to company, companyId={}", companyId);
    }

    // =========================================================================
    // UC II.4.3 — Set discount rules for EVENT policy
    // =========================================================================
    public void setDiscountRulesForEvent(String actorToken, int companyId, UUID eventId,
                                          List<DiscountRule> rules, boolean isAdditive) {
        logger.info("Set discount rules for event, companyId={}, eventId={}", companyId, eventId);

        Member  actor   = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);

        DiscountPolicy policy = findDiscountPolicyByEventId(eventId).orElseThrow(() -> {
            logger.error("No discount policy found for eventId={}", eventId);
            return new IllegalArgumentException("No discount policy found for eventId=" + eventId);
        });

        policyAuthService.assertCanModifyDiscountPolicy(actor, company, policy);
        policy.setRules(rules, isAdditive);
        policyRepo.savePolicy(policy);
        logger.info("Discount rules set for event, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Set discount rules for COMPANY policy
    // =========================================================================
    public void setDiscountRulesForCompany(String actorToken, int companyId,
                                            List<DiscountRule> rules, boolean isAdditive) {
        logger.info("Set discount rules for company, companyId={}", companyId);

        Member  actor   = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);

        DiscountPolicy policy = findDiscountPolicyByCompanyId(companyId).orElseThrow(() -> {
            logger.error("No company discount policy found for companyId={}", companyId);
            return new IllegalArgumentException("No company discount policy found for companyId=" + companyId);
        });

        policyAuthService.assertCanModifyDiscountPolicy(actor, company, policy);
        policy.setRules(rules, isAdditive);
        policyRepo.savePolicy(policy);
        logger.info("Discount rules set for company, companyId={}", companyId);
    }

    // =========================================================================
    // UC II.4.3 — Add purchase rule to EVENT policy
    // =========================================================================
    public void addPurchaseRuleToEvent(String actorToken, int companyId,
                                       UUID eventId, PurchaseRule newRule) {
        logger.info("Add purchase rule to event, companyId={}, eventId={}", companyId, eventId);

        Member  actor   = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);

        PurchasePolicy policy = findPurchasePolicyByEventId(eventId).orElseThrow(() -> {
            logger.error("No purchase policy found for eventId={}", eventId);
            return new IllegalArgumentException("No purchase policy found for eventId=" + eventId);
        });

        policyAuthService.assertCanModifyPurchasePolicy(actor, company, policy);
        policy.addRule(newRule);
        policyRepo.savePolicy(policy);
        logger.info("Purchase rule added to event, eventId={}", eventId);
    }

    // =========================================================================
    // UC II.4.3 — Add purchase rule to COMPANY policy
    // =========================================================================
    public void addPurchaseRuleToCompany(String actorToken, int companyId, PurchaseRule newRule) {
        logger.info("Add purchase rule to company, companyId={}", companyId);

        Member  actor   = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);

        PurchasePolicy policy = findPurchasePolicyByCompanyId(companyId).orElseThrow(() -> {
            logger.error("No company purchase policy found for companyId={}", companyId);
            return new IllegalArgumentException("No company purchase policy found for companyId=" + companyId);
        });

        policyAuthService.assertCanModifyPurchasePolicy(actor, company, policy);
        policy.addRule(newRule);
        policyRepo.savePolicy(policy);
        logger.info("Purchase rule added to company, companyId={}", companyId);
    }

    // =========================================================================
    // UC II.4.3 — Set purchase rules for EVENT / COMPANY policy
    // =========================================================================
    public void setPurchaseRulesForEvent(String actorToken, int companyId, UUID eventId,
                                          List<PurchaseRule> rules, PurchasePolicy.Operator operator) {
        logger.info("Set purchase rules for event, companyId={}, eventId={}", companyId, eventId);

        Member  actor   = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);

        PurchasePolicy policy = findPurchasePolicyByEventId(eventId).orElseThrow(() -> {
            logger.error("No purchase policy found for eventId={}", eventId);
            return new IllegalArgumentException("No purchase policy found for eventId=" + eventId);
        });

        policyAuthService.assertCanModifyPurchasePolicy(actor, company, policy);
        policy.setRules(rules, operator);
        policyRepo.savePolicy(policy);
        logger.info("Purchase rules set for event, eventId={}", eventId);
    }

    public void setPurchaseRulesForCompany(String actorToken, int companyId,
                                            List<PurchaseRule> rules, PurchasePolicy.Operator operator) {
        logger.info("Set purchase rules for company, companyId={}", companyId);

        Member  actor   = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);

        PurchasePolicy policy = findPurchasePolicyByCompanyId(companyId).orElseThrow(() -> {
            logger.error("No company purchase policy found for companyId={}", companyId);
            return new IllegalArgumentException("No company purchase policy found for companyId=" + companyId);
        });

        policyAuthService.assertCanModifyPurchasePolicy(actor, company, policy);
        policy.setRules(rules, operator);
        policyRepo.savePolicy(policy);
        logger.info("Purchase rules set for company, companyId={}", companyId);
    }

    // =========================================================================
    // Private helpers — find with event→company fallback
    // =========================================================================

    private Optional<DiscountPolicy> findDiscountPolicy(int companyId, UUID eventId) {
        if (eventId != null) {
            Optional<DiscountPolicy> ep = findDiscountPolicyByEventId(eventId);
            if (ep.isPresent()) return ep;
        }
        return findDiscountPolicyByCompanyId(companyId);
    }

    private Optional<PurchasePolicy> findPurchasePolicy(int companyId, UUID eventId) {
        if (eventId != null) {
            Optional<PurchasePolicy> ep = findPurchasePolicyByEventId(eventId);
            if (ep.isPresent()) return ep;
        }
        return findPurchasePolicyByCompanyId(companyId);
    }

    private Optional<SellingPolicy> findSellingPolicy(int companyId, UUID eventId) {
        if (eventId != null) {
            Object result = policyRepo.findSellingPolicyByEventId(eventId);
            Optional<SellingPolicy> ep = toOptional(result);
            if (ep.isPresent()) return ep;
        }
        return policyRepo.findSellingPolicyByCompanyIdAndEventIdIsNull(companyId);
    }

    private Optional<DiscountPolicy> findDiscountPolicyByEventId(UUID eventId) {
        Object result = policyRepo.findDiscountPolicyByEventId(eventId);
        return toOptional(result);
    }

    private Optional<DiscountPolicy> findDiscountPolicyByCompanyId(int companyId) {
        return policyRepo.findDiscountPolicyByCompanyIdAndEventIdIsNull(companyId);
    }

    private Optional<PurchasePolicy> findPurchasePolicyByEventId(UUID eventId) {
        Object result = policyRepo.findPurchasePolicyByEventId(eventId);
        return toOptional(result);
    }

    private Optional<PurchasePolicy> findPurchasePolicyByCompanyId(int companyId) {
        return policyRepo.findPurchasePolicyByCompanyIdAndEventIdIsNull(companyId);
    }

    /**
     * Handles both null (old mocks) and Optional (new repository).
     */
    @SuppressWarnings("unchecked")
    private <T> Optional<T> toOptional(Object result) {
        if (result == null)                 return Optional.empty();
        if (result instanceof Optional)     return (Optional<T>) result;
        return Optional.of((T) result);
    }

    private Member getActorFromToken(String actorToken) {
        if (actorToken == null || actorToken.isBlank()) {
            logger.error("Invalid actor token: '{}'", actorToken);
            throw new SecurityException("Invalid token");
        }
            
        return representUserService.requireMember(actorToken);
    }

    private Company getCompanyOrThrow(int companyId) {
        return companyRepo.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Company ID " + companyId + " not found."));
    }
}
