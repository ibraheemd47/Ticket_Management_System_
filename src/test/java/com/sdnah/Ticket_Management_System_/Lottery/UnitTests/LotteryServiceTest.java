package com.sdnah.Ticket_Management_System_.Lottery.UnitTests;


import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.LotteryService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryEntryDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LotteryServiceTest {

    private LotteryService lotteryService;
    private LotteryRepository lotteryRepository;
    private CompanyRepository companyRepository;
    private IrepresnteUserService representUserService;
    private Member ownerMember;
    private Member regularMember;
    private Company company;
    private final int COMPANY_ID = 1;
    private final UUID EVENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lotteryRepository    = mock(LotteryRepository.class);
        companyRepository    = mock(CompanyRepository.class);
        representUserService = mock(IrepresnteUserService.class);

        lotteryService = new LotteryService(
                lotteryRepository,
                companyRepository,
                representUserService,
                new KeyedLock());

        ownerMember = mock(Member.class);
        when(ownerMember.getMemberId()).thenReturn("owner-1");
        when(ownerMember.isActive()).thenReturn(true);
        when(ownerMember.isVerified()).thenReturn(true);

        regularMember = mock(Member.class);
        when(regularMember.getMemberId()).thenReturn("member-1");
        when(regularMember.isActive()).thenReturn(true);
        when(regularMember.isVerified()).thenReturn(true);

        company = mock(Company.class);
        when(company.isOwner("owner-1")).thenReturn(true);
        when(company.isOwner("member-1")).thenReturn(false);

        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
    }

    @Test
    void givenOwnerToken_WhenCreatingLottery_ThenLotteryDTOIsReturned() {
        when(representUserService.requireMember("owner-token")).thenReturn(ownerMember);

        LotteryDTO dto = lotteryService.createLottery(
                "owner-token", EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));

        assertNotNull(dto);
        assertEquals(EVENT_ID, dto.getEventId());
        verify(lotteryRepository).save(any());
    }

    @Test
    void givenNonOwnerToken_WhenCreatingLottery_ThenRuntimeExceptionIsThrown() {
        when(representUserService.requireMember("member-token")).thenReturn(regularMember);

        assertThrows(RuntimeException.class, () ->
                lotteryService.createLottery(
                        "member-token", EVENT_ID, COMPANY_ID,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2)));
    }

    @Test
    void givenVerifiedMember_WhenRegisteringToOpenLottery_ThenEntryDTOIsReturned() {
        when(representUserService.requireMember("member-token")).thenReturn(regularMember);

        Lottery lottery = new Lottery(EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));

        when(lotteryRepository.findById(lottery.getId())).thenReturn(Optional.of(lottery));

        LotteryEntryDTO entry = lotteryService.registerToLottery("member-token", lottery.getId());

        assertNotNull(entry);
        assertEquals("member-1", entry.getMemberId());
        assertFalse(entry.isWinner());
    }

    @Test
    void givenNonExistentLottery_WhenRegistering_ThenIllegalArgumentExceptionIsThrown() {
        when(representUserService.requireMember("member-token")).thenReturn(regularMember);
        when(lotteryRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> lotteryService.registerToLottery("member-token", UUID.randomUUID()));
    }

    @Test
    void givenOwnerAndEntries_WhenDrawing_ThenWinnersAreReturned() {
        when(representUserService.requireMember("owner-token")).thenReturn(ownerMember);

        Lottery lottery = new Lottery(EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
        lottery.register("member-1");
        lottery.register("member-2");

        when(lotteryRepository.findById(lottery.getId())).thenReturn(Optional.of(lottery));

        List<LotteryEntryDTO> winners = lotteryService.drawLottery("owner-token", lottery.getId(), 1);

        assertEquals(1, winners.size());
        assertTrue(winners.get(0).isWinner());
    }

    @Test
    void givenNonOwner_WhenDrawing_ThenRuntimeExceptionIsThrown() {
        when(representUserService.requireMember("member-token")).thenReturn(regularMember);

        Lottery lottery = new Lottery(EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
        lottery.register("member-1");

        when(lotteryRepository.findById(any())).thenReturn(Optional.of(lottery));

        assertThrows(RuntimeException.class,
                () -> lotteryService.drawLottery("member-token", lottery.getId(), 1));
    }

    @Test
    void givenNonExistentLottery_WhenDrawing_ThenIllegalArgumentExceptionIsThrown() {
        when(representUserService.requireMember("owner-token")).thenReturn(ownerMember);
        when(lotteryRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> lotteryService.drawLottery("owner-token", UUID.randomUUID(), 1));
    }
}