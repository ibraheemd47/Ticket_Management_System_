package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.RuleResult;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Leaf Purchase Rules — Domain Unit Tests")
class PurchaseLeafRulesTest {

    private PurchaseContext ctx(int age, int qty) {
        return new PurchaseContext(age, qty, false, true, null);
    }

    // =========================================================================
    // MinAgeRule
    // =========================================================================

    @Nested
    @DisplayName("MinAgeRule")
    class MinAgeRuleTests {

        @Test
        void GivenMinAge18_WhenBuyerExactlyAge18_ThenAllowed() {
            MinAgeRule rule = new MinAgeRule(18);

            RuleResult result = rule.evaluate(ctx(18, 1));

            assertTrue(result.isAllowed());
        }

        @Test
        void GivenMinAge18_WhenBuyerYoungerThan18_ThenDeniedWithMessage() {
            MinAgeRule rule = new MinAgeRule(18);

            RuleResult result = rule.evaluate(ctx(16, 1));

            assertFalse(result.isAllowed());
            assertTrue(result.getMessage().contains("18"));
        }

        @Test
        void GivenMinAge0_WhenAnyAge_ThenAllowed() {
            MinAgeRule rule = new MinAgeRule(0);

            assertTrue(rule.evaluate(ctx(0, 1)).isAllowed());
        }

        @Test
        void GivenNegativeMinAge_WhenConstructed_ThenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> new MinAgeRule(-1));
        }

        @Test
        void GivenMinAgeRule_WhenDescribe_ThenContainsAgeValue() {
            MinAgeRule rule = new MinAgeRule(21);

            assertTrue(rule.describe().contains("21"));
        }

        @Test
        void GivenMinAgeRule_WhenGetMinimumAge_ThenReturnsValue() {
            MinAgeRule rule = new MinAgeRule(18);

            assertEquals(18, rule.getMinimumAge());
        }
    }

    // =========================================================================
    // MinTicketsRule
    // =========================================================================

    @Nested
    @DisplayName("MinTicketsRule")
    class MinTicketsRuleTests {

        @Test
        void GivenMinTickets2_WhenQuantityExactly2_ThenAllowed() {
            MinTicketsRule rule = new MinTicketsRule(2);

            assertTrue(rule.evaluate(ctx(20, 2)).isAllowed());
        }

        @Test
        void GivenMinTickets2_WhenQuantity1_ThenDeniedWithMessage() {
            MinTicketsRule rule = new MinTicketsRule(2);

            RuleResult result = rule.evaluate(ctx(20, 1));

            assertFalse(result.isAllowed());
            assertTrue(result.getMessage().contains("2"));
        }

        @Test
        void GivenZeroMinTickets_WhenConstructed_ThenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> new MinTicketsRule(0));
        }

        @Test
        void GivenNegativeMinTickets_WhenConstructed_ThenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> new MinTicketsRule(-3));
        }

        @Test
        void GivenMinTicketsRule_WhenDescribe_ThenContainsQuantity() {
            MinTicketsRule rule = new MinTicketsRule(5);

            assertTrue(rule.describe().contains("5"));
        }

        @Test
        void GivenMinTicketsRule_WhenGetMinTickets_ThenReturnsValue() {
            MinTicketsRule rule = new MinTicketsRule(4);

            assertEquals(4, rule.getMinTickets());
        }
    }

    // =========================================================================
    // MaxTicketsRule
    // =========================================================================

    @Nested
    @DisplayName("MaxTicketsRule")
    class MaxTicketsRuleTests {

        @Test
        void GivenMaxTickets5_WhenQuantityExactly5_ThenAllowed() {
            MaxTicketsRule rule = new MaxTicketsRule(5);

            assertTrue(rule.evaluate(ctx(20, 5)).isAllowed());
        }

        @Test
        void GivenMaxTickets5_WhenQuantity6_ThenDeniedWithMessage() {
            MaxTicketsRule rule = new MaxTicketsRule(5);

            RuleResult result = rule.evaluate(ctx(20, 6));

            assertFalse(result.isAllowed());
            assertTrue(result.getMessage().contains("5"));
        }

        @Test
        void GivenZeroMaxTickets_WhenConstructed_ThenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> new MaxTicketsRule(0));
        }

        @Test
        void GivenNegativeMaxTickets_WhenConstructed_ThenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> new MaxTicketsRule(-2));
        }

        @Test
        void GivenMaxTicketsRule_WhenDescribe_ThenContainsQuantity() {
            MaxTicketsRule rule = new MaxTicketsRule(10);

            assertTrue(rule.describe().contains("10"));
        }

        @Test
        void GivenMaxTicketsRule_WhenGetMaxTickets_ThenReturnsValue() {
            MaxTicketsRule rule = new MaxTicketsRule(7);

            assertEquals(7, rule.getMaxTickets());
        }
    }
}