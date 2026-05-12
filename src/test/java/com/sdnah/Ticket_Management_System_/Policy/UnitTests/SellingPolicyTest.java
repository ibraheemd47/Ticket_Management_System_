package com.sdnah.Ticket_Management_System_.Policy.UnitTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SellingPolicy — Domain Unit Tests")
class SellingPolicyTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Test
    void GivenRegularSellingPolicy_WhenGuestSelectsTickets_ThenReturnTrue() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID
        );

        assertTrue(policy.isSelectionAllowed(false));
    }

    @Test
    void GivenRegularSellingPolicy_WhenMemberSelectsTickets_ThenReturnTrue() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID
        );

        assertTrue(policy.isSelectionAllowed(true));
    }

    @Test
    void GivenLotterySellingPolicy_WhenGuestSelectsTickets_ThenReturnFalse() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Lottery",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID
        );

        assertFalse(policy.isSelectionAllowed(false));
    }

    @Test
    void GivenLotterySellingPolicy_WhenMemberSelectsTickets_ThenReturnTrue() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Lottery",
                SellingPolicy.SellingType.LOTTERY,
                EVENT_ID
        );

        assertTrue(policy.isSelectionAllowed(true));
    }

    @Test
    void GivenSellingPolicy_WhenSetTypeToLottery_ThenGuestSelectionRejected() {
        SellingPolicy policy = new SellingPolicy(
                1,
                "Regular",
                SellingPolicy.SellingType.REGULAR,
                EVENT_ID
        );

        policy.setType(SellingPolicy.SellingType.LOTTERY);

        assertFalse(policy.isSelectionAllowed(false));
    }
}