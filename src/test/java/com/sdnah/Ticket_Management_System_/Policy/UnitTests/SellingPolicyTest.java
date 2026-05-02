package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.SellingPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;

@DisplayName("SellingPolicy — Domain Unit Tests")
class SellingPolicyTest {

    private static final int COMPANY_ID = 10;
    private static final int EVENT_ID = 20;

    @Test
    void GivenRegularSellingPolicy_WhenGuestSelectsTickets_ThenReturnTrue() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID,
                COMPANY_ID
        );

        assertTrue(policy.isSelectionAllowed(false));
    }

    @Test
    void GivenRegularSellingPolicy_WhenMemberSelectsTickets_ThenReturnTrue() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID,
                COMPANY_ID
        );

        assertTrue(policy.isSelectionAllowed(true));
    }

    @Test
    void GivenLotterySellingPolicy_WhenGuestSelectsTickets_ThenReturnFalse() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Lottery",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID,
                COMPANY_ID
        );

        assertFalse(policy.isSelectionAllowed(false));
    }

    @Test
    void GivenLotterySellingPolicy_WhenMemberSelectsTickets_ThenReturnTrue() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Lottery",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID,
                COMPANY_ID
        );

        assertTrue(policy.isSelectionAllowed(true));
    }

    @Test
    void GivenSellingPolicy_WhenIsValid_ThenReturnTrue() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID,
                COMPANY_ID
        );

        assertTrue(policy.isValid());
    }
}