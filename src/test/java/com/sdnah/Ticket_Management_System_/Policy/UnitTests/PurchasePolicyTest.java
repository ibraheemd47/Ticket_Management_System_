package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase.RuleResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PurchasePolicy — Domain Unit Tests")
class PurchasePolicyTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    // =========================================================================
    // Default policy (no rules)
    // =========================================================================

    @Test
    void GivenDefaultPurchasePolicy_WhenQuantityOne_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID);

        assertTrue(policy.validatePurchase(1, 20, false));
    }

    @Test
    void GivenDefaultPurchasePolicy_WhenQuantityZero_ThenExceptionThrown() {
        PurchasePolicy policy = new PurchasePolicy(1, "Default", EVENT_ID);

        // DiscountContext rejects quantity <= 0
        assertThrows(IllegalArgumentException.class,
                () -> policy.validatePurchase(0, 20, false));
    }

    // =========================================================================
    // MaxTicketsRule
    // =========================================================================

    @Test
    void GivenMaxTicketsTwo_WhenQuantityThree_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Max two", EVENT_ID);
        policy.addRule(new MaxTicketsRule(2));

        assertFalse(policy.validatePurchase(3, 20, false));
    }

    @Test
    void GivenMaxTicketsTwo_WhenQuantityTwo_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Max two", EVENT_ID);
        policy.addRule(new MaxTicketsRule(2));

        assertTrue(policy.validatePurchase(2, 20, false));
    }

    // =========================================================================
    // MinTicketsRule
    // =========================================================================

    @Test
    void GivenMinTicketsTwo_WhenQuantityOne_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Min two", EVENT_ID);
        policy.addRule(new MinTicketsRule(2));

        assertFalse(policy.validatePurchase(1, 20, false));
    }

    @Test
    void GivenMinTicketsTwo_WhenQuantityTwo_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Min two", EVENT_ID);
        policy.addRule(new MinTicketsRule(2));

        assertTrue(policy.validatePurchase(2, 20, false));
    }

    // =========================================================================
    // MinAgeRule
    // =========================================================================

    @Test
    void GivenMinAge18_WhenBuyerAge16_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "18+ policy", EVENT_ID);
        policy.addRule(new MinAgeRule(18));

        assertFalse(policy.validatePurchase(1, 16, false));
    }

    @Test
    void GivenMinAge18_WhenBuyerAge18_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "18+ policy", EVENT_ID);
        policy.addRule(new MinAgeRule(18));

        assertTrue(policy.validatePurchase(1, 18, false));
    }

    // =========================================================================
    // AND / OR composition
    // =========================================================================

    @Test
    void GivenAndRule_WhenAllConditionsMet_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Age 18+ AND max 5", EVENT_ID);
        policy.setRules(List.of(new MinAgeRule(18), new MaxTicketsRule(5)),
                PurchasePolicy.Operator.AND);

        assertTrue(policy.validatePurchase(3, 20, false));
    }

    @Test
    void GivenAndRule_WhenOneConditionFails_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Age 18+ AND max 5", EVENT_ID);
        policy.setRules(List.of(new MinAgeRule(18), new MaxTicketsRule(5)),
                PurchasePolicy.Operator.AND);

        assertFalse(policy.validatePurchase(6, 20, false)); // qty fails
    }

    @Test
    void GivenOrRule_WhenOneConditionMet_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Max 2 OR Min 100", EVENT_ID);
        policy.setRules(List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);

        assertTrue(policy.validatePurchase(1, 20, false));   // satisfies MaxTickets
        assertTrue(policy.validatePurchase(100, 20, false)); // satisfies MinTickets
    }

    @Test
    void GivenOrRule_WhenNoConditionMet_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy(1, "Max 2 OR Min 100", EVENT_ID);
        policy.setRules(List.of(new MaxTicketsRule(2), new MinTicketsRule(100)),
                PurchasePolicy.Operator.OR);

        assertFalse(policy.validatePurchase(5, 20, false)); // fails both
    }

    // =========================================================================
    // validate — RuleResult message
    // =========================================================================

    @Test
    void GivenMinAgeRule_WhenBuyerTooYoung_ThenDenialMessageContainsAge() {
        PurchasePolicy policy = new PurchasePolicy(1, "18+ policy", EVENT_ID);
        policy.addRule(new MinAgeRule(18));

        RuleResult result = policy.validate(
                new com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase
                        .PurchaseContext(16, 1, false, false, null));

        assertFalse(result.isAllowed());
        assertTrue(result.getMessage().contains("18"));
    }

    // =========================================================================
    // clearRules
    // =========================================================================

    @Test
    void GivenPolicyWithRules_WhenClearRules_ThenAllPurchasesAllowed() {
        PurchasePolicy policy = new PurchasePolicy(1, "With rules", EVENT_ID);
        policy.addRule(new MinAgeRule(18));

        policy.clearRules();

        assertTrue(policy.validatePurchase(1, 16, false)); // no rules = allowed
    }

    // =========================================================================
    // isValid
    // =========================================================================

    @Test
    void GivenValidPolicy_WhenIsValid_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy(1, "Valid", EVENT_ID);

        assertTrue(policy.isValid());
    }
}