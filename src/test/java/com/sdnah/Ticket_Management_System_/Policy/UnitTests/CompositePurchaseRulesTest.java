package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.AndRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MaxTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinAgeRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.MinTicketsRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.OrRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.RuleResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Composite Purchase Rules — Domain Unit Tests")
class CompositePurchaseRulesTest {

    private PurchaseContext ctx(int age, int qty) {
        return new PurchaseContext(age, qty, false, true, null);
    }

    // =========================================================================
    // AndRule
    // =========================================================================

    @Nested
    @DisplayName("AndRule")
    class AndRuleTests {

        @Test
        void GivenAndRule_WhenAllChildrenPass_ThenAllowed() {
            AndRule and = new AndRule(new MinAgeRule(18), new MaxTicketsRule(5));

            assertTrue(and.evaluate(ctx(20, 3)).isAllowed());
        }

        @Test
        void GivenAndRule_WhenFirstChildFails_ThenDeniedWithFirstChildMessage() {
            AndRule and = new AndRule(new MinAgeRule(18), new MaxTicketsRule(5));

            RuleResult result = and.evaluate(ctx(16, 3));

            assertFalse(result.isAllowed());
            assertTrue(result.getMessage().contains("18"));
        }

        @Test
        void GivenAndRule_WhenSecondChildFails_ThenDeniedWithSecondChildMessage() {
            AndRule and = new AndRule(new MinAgeRule(18), new MaxTicketsRule(5));

            RuleResult result = and.evaluate(ctx(20, 10));

            assertFalse(result.isAllowed());
            assertTrue(result.getMessage().contains("5"));
        }

        @Test
        void GivenAndRule_WhenListConstructor_ThenStoresAllChildren() {
            AndRule and = new AndRule(List.of(
                    new MinAgeRule(18),
                    new MinTicketsRule(2),
                    new MaxTicketsRule(5)));

            assertEquals(3, and.getRules().size());
        }

        @Test
        void GivenAndRule_WhenNullList_ThenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> new AndRule((List<PurchaseRule>) null));
        }

        @Test
        void GivenAndRule_WhenEmptyList_ThenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> new AndRule(List.of()));
        }

        @Test
        void GivenAndRule_WhenDescribe_ThenContainsAllChildDescriptions() {
            AndRule and = new AndRule(new MinAgeRule(18), new MaxTicketsRule(5));

            String desc = and.describe();
            assertTrue(desc.contains("18"));
            assertTrue(desc.contains("5"));
            assertTrue(desc.contains("AND"));
        }

        @Test
        void GivenAndRule_WhenGetRules_ThenReturnsImmutableCopy() {
            AndRule and = new AndRule(new MinAgeRule(18), new MaxTicketsRule(5));

            assertThrows(UnsupportedOperationException.class,
                    () -> and.getRules().add(new MinTicketsRule(1)));
        }
    }

    // =========================================================================
    // OrRule
    // =========================================================================

    @Nested
    @DisplayName("OrRule")
    class OrRuleTests {

        @Test
        void GivenOrRule_WhenAnyChildPasses_ThenAllowed() {
            OrRule or = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));

            assertTrue(or.evaluate(ctx(20, 1)).isAllowed());
            assertTrue(or.evaluate(ctx(20, 100)).isAllowed());
        }

        @Test
        void GivenOrRule_WhenAllChildrenFail_ThenDenied() {
            OrRule or = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));

            assertFalse(or.evaluate(ctx(20, 5)).isAllowed());
        }

        @Test
        void GivenOrRule_WhenAllFail_ThenMessageContainsAllReasons() {
            OrRule or = new OrRule(new MinAgeRule(18), new MinTicketsRule(10));

            RuleResult result = or.evaluate(ctx(16, 2));

            assertFalse(result.isAllowed());
            assertTrue(result.getMessage().contains("18"));
            assertTrue(result.getMessage().contains("10"));
        }

        @Test
        void GivenOrRule_WhenListConstructor_ThenStoresAllChildren() {
            OrRule or = new OrRule(List.of(
                    new MaxTicketsRule(2),
                    new MinTicketsRule(100)));

            assertEquals(2, or.getRules().size());
        }

        @Test
        void GivenOrRule_WhenEmptyList_ThenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> new OrRule(List.of()));
        }

        @Test
        void GivenOrRule_WhenDescribe_ThenContainsOrSeparator() {
            OrRule or = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));

            String desc = or.describe();
            assertTrue(desc.contains("OR"));
            assertTrue(desc.contains("2"));
            assertTrue(desc.contains("100"));
        }
    }
}