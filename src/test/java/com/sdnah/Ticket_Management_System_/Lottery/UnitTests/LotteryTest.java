package com.sdnah.Ticket_Management_System_.Lottery.UnitTests;


import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.LotteryEntry;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.LotteryAuthDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LotteryTest {

    private UUID eventId;
    private UUID companyId;
    private LocalDateTime deadline;
    private LocalDateTime drawTime;
    private LotteryAuthDomainService lotteryAuth;
    private Member activeMember;
    private Member inactiveMember;
    private Company company;
    private Lottery openLottery;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        companyId = UUID.randomUUID();;
        deadline = LocalDateTime.now().plusDays(1);
        drawTime = LocalDateTime.now().plusDays(2);
        lotteryAuth = new LotteryAuthDomainService();

        activeMember = mock(Member.class);
        when(activeMember.isActive()).thenReturn(true);
        when(activeMember.isVerified()).thenReturn(true);
        when(activeMember.getMemberId()).thenReturn("member-1");

        inactiveMember = mock(Member.class);
        when(inactiveMember.isActive()).thenReturn(false);

        company = mock(Company.class);
        when(company.isOwner("member-1")).thenReturn(true);

        openLottery = new Lottery(eventId, companyId, deadline, drawTime);
    }

    // =========================================================================
    // Lottery creation
    // =========================================================================

    @Test
    void givenValidParams_WhenCreatingLottery_ThenLotteryIsOpen() {
        Lottery lottery = new Lottery(eventId, companyId, deadline, drawTime);
        assertEquals(Lottery.LotteryStatus.OPEN, lottery.getStatus());
    }

    @Test
    void givenNullEventId_WhenCreatingLottery_ThenIllegalArgumentExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lottery(null, companyId, deadline, drawTime));
    }

    @Test
    void givenInvalidCompanyId_WhenCreatingLottery_ThenIllegalArgumentExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lottery(eventId, null, deadline, drawTime));
    }

    @Test
    void givenDrawTimeBeforeDeadline_WhenCreatingLottery_ThenIllegalArgumentExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lottery(eventId, companyId, drawTime, deadline));
    }

    // =========================================================================
    // Register
    // =========================================================================

    @Test
    void givenOpenLottery_WhenMemberRegisters_ThenEntryIsAdded() {
        LotteryEntry entry = openLottery.register("member-1");
        assertNotNull(entry);
        assertEquals("member-1", entry.getMemberId());
        assertEquals(1, openLottery.getEntries().size());
    }

    @Test
    void givenMemberAlreadyRegistered_WhenRegisteringAgain_ThenIllegalStateExceptionIsThrown() {
        openLottery.register("member-1");
        assertThrows(IllegalStateException.class,
                () -> openLottery.register("member-1"));
    }

    @Test
    void givenNullMemberId_WhenRegistering_ThenIllegalArgumentExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class,
                () -> openLottery.register(null));
    }

    @Test
    void givenBlankMemberId_WhenRegistering_ThenIllegalArgumentExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class,
                () -> openLottery.register("  "));
    }

    // =========================================================================
    // Draw
    // =========================================================================

    @Test
    void givenOpenLotteryWithEntries_WhenDrawing_ThenWinnersAreMarked() {
        openLottery.register("member-1");
        openLottery.register("member-2");
        openLottery.register("member-3");

        List<LotteryEntry> winners = openLottery.draw(2);

        assertEquals(2, winners.size());
        winners.forEach(w -> assertTrue(w.isWinner()));
        assertEquals(Lottery.LotteryStatus.DRAWN, openLottery.getStatus());
    }

    @Test
    void givenFewerEntriesThanWinnersCount_WhenDrawing_ThenAllEntriesWin() {
        openLottery.register("member-1");
        List<LotteryEntry> winners = openLottery.draw(5);
        assertEquals(1, winners.size());
    }

    @Test
    void givenDrawnLottery_WhenDrawingAgain_ThenIllegalStateExceptionIsThrown() {
        openLottery.register("member-1");
        openLottery.draw(1);
        assertThrows(IllegalStateException.class, () -> openLottery.draw(1));
    }

    @Test
    void givenInvalidWinnersCount_WhenDrawing_ThenIllegalArgumentExceptionIsThrown() {
        openLottery.register("member-1");
        assertThrows(IllegalArgumentException.class, () -> openLottery.draw(0));
    }

    // =========================================================================
    // LotteryEntry
    // =========================================================================

    @Test
    void givenNewEntry_WhenCreated_ThenNotWinner() {
        LotteryEntry entry = openLottery.register("member-1");
        assertFalse(entry.isWinner());
        assertNull(entry.getAccessCode());
    }

    @Test
    void givenEntry_WhenMarkedAsWinner_ThenHasValidAccessCode() {
        LotteryEntry entry = openLottery.register("member-1");
        entry.markAsWinner();
        assertTrue(entry.isWinner());
        assertNotNull(entry.getAccessCode());
        assertTrue(entry.isAccessCodeValid());
    }

    @Test
    void givenNonWinnerEntry_WhenCheckingAccessCode_ThenReturnsFalse() {
        LotteryEntry entry = openLottery.register("member-1");
        assertFalse(entry.isAccessCodeValid());
    }

    // =========================================================================
    // LotteryAuthDomainService
    // =========================================================================

    @Test
    void givenOwner_WhenCreatingLottery_ThenNoException() {
        assertDoesNotThrow(() -> lotteryAuth.assertCanCreateLottery(activeMember, company));
    }

    @Test
    void givenInactiveMember_WhenCreatingLottery_ThenRuntimeExceptionIsThrown() {
        assertThrows(RuntimeException.class,
                () -> lotteryAuth.assertCanCreateLottery(inactiveMember, company));
    }

    @Test
    void givenNonOwner_WhenCreatingLottery_ThenRuntimeExceptionIsThrown() {
        when(company.isOwner("member-1")).thenReturn(false);
        assertThrows(RuntimeException.class,
                () -> lotteryAuth.assertCanCreateLottery(activeMember, company));
    }

    @Test
    void givenNullCompany_WhenCreatingLottery_ThenRuntimeExceptionIsThrown() {
        assertThrows(RuntimeException.class,
                () -> lotteryAuth.assertCanCreateLottery(activeMember, null));
    }

    @Test
    void givenOwner_WhenDrawingLottery_ThenNoException() {
        assertDoesNotThrow(() -> lotteryAuth.assertCanDrawLottery(activeMember, company));
    }

    @Test
    void givenNonOwner_WhenDrawingLottery_ThenRuntimeExceptionIsThrown() {
        when(company.isOwner("member-1")).thenReturn(false);
        assertThrows(RuntimeException.class,
                () -> lotteryAuth.assertCanDrawLottery(activeMember, company));
    }

    @Test
    void givenVerifiedActiveMember_WhenRegisteringToOpenLottery_ThenNoException() {
        assertDoesNotThrow(() -> lotteryAuth.assertCanRegisterToLottery(activeMember, openLottery));
    }

    @Test
    void givenInactiveMember_WhenRegisteringToLottery_ThenRuntimeExceptionIsThrown() {
        assertThrows(RuntimeException.class,
                () -> lotteryAuth.assertCanRegisterToLottery(inactiveMember, openLottery));
    }

    @Test
    void givenUnverifiedMember_WhenRegisteringToLottery_ThenRuntimeExceptionIsThrown() {
        when(activeMember.isVerified()).thenReturn(false);
        assertThrows(RuntimeException.class,
                () -> lotteryAuth.assertCanRegisterToLottery(activeMember, openLottery));
    }

    @Test
    void givenAlreadyRegisteredMember_WhenRegisteringAgain_ThenRuntimeExceptionIsThrown() {
        openLottery.register("member-1");
        assertThrows(RuntimeException.class,
                () -> lotteryAuth.assertCanRegisterToLottery(activeMember, openLottery));
    }
}