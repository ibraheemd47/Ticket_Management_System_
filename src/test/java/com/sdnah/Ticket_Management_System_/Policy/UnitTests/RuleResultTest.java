package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.RuleResult;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuleResult — Value Object Unit Tests")
class RuleResultTest {

    @Test
    void GivenAllowedFactory_WhenCalled_ThenReturnsAllowedResult() {
        RuleResult result = RuleResult.allowed();

        assertTrue(result.isAllowed());
        assertEquals("Purchase allowed.", result.getMessage());
    }

    @Test
    void GivenDeniedFactory_WhenCalledWithReason_ThenReturnsDeniedResult() {
        RuleResult result = RuleResult.denied("Buyer too young");

        assertFalse(result.isAllowed());
        assertEquals("Buyer too young", result.getMessage());
    }

    @Test
    void GivenDeniedFactory_WhenCalledWithNullReason_ThenThrowException() {
        assertThrows(IllegalArgumentException.class, () -> RuleResult.denied(null));
    }

    @Test
    void GivenDeniedFactory_WhenCalledWithBlankReason_ThenThrowException() {
        assertThrows(IllegalArgumentException.class, () -> RuleResult.denied("   "));
    }

    @Test
    void GivenDeniedFactory_WhenCalledWithEmptyReason_ThenThrowException() {
        assertThrows(IllegalArgumentException.class, () -> RuleResult.denied(""));
    }

    @Test
    void GivenAllowedResult_WhenToString_ThenContainsAllowedPrefix() {
        assertTrue(RuleResult.allowed().toString().startsWith("[ALLOWED]"));
    }

    @Test
    void GivenDeniedResult_WhenToString_ThenContainsDeniedPrefixAndReason() {
        RuleResult result = RuleResult.denied("Too young");

        assertTrue(result.toString().startsWith("[DENIED]"));
        assertTrue(result.toString().contains("Too young"));
    }
}