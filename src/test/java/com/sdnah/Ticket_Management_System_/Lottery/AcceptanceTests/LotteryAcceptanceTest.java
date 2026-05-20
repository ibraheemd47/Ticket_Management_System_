package com.sdnah.Ticket_Management_System_.Lottery.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT) // ←
@DisplayName("LotteryService — Acceptance Tests")
public class LotteryAcceptanceTest {

    @Mock private LotteryRepository lotteryRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private IrepresnteUserService representUserService;

    private LotteryService lotteryService;

    private Member ownerMember;
    private Member regularMember;
    private Company company;

    private static final int COMPANY_ID = 1;
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final String OWNER_TOKEN = "owner-token";
    private static final String MEMBER_TOKEN = "member-token";

    @BeforeEach
    void setUp() {
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

    // =========================================================================
    // UC II.3.6 — Create Lottery
    // =========================================================================

    @Test
    @DisplayName("Given owner token, when creating lottery, then lottery DTO is returned")
    void givenOwnerToken_WhenCreatingLottery_ThenLotteryDTOIsReturned() {
        when(representUserService.requireMember(OWNER_TOKEN)).thenReturn(ownerMember);

        LotteryDTO dto = lotteryService.createLottery(
                OWNER_TOKEN, EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));

        assertNotNull(dto);
        assertEquals(EVENT_ID, dto.getEventId());
        assertEquals(COMPANY_ID, dto.getCompanyId());
        assertEquals(Lottery.LotteryStatus.OPEN, dto.getStatus());
        verify(lotteryRepository).save(any());
    }

    @Test
    @DisplayName("Given non-owner token, when creating lottery, then exception is thrown")
    void givenNonOwnerToken_WhenCreatingLottery_ThenExceptionIsThrown() {
        when(representUserService.requireMember(MEMBER_TOKEN)).thenReturn(regularMember);

        assertThrows(RuntimeException.class, () ->
                lotteryService.createLottery(
                        MEMBER_TOKEN, EVENT_ID, COMPANY_ID,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2)));

        verify(lotteryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given draw time before deadline, when creating lottery, then exception is thrown")
    void givenDrawTimeBeforeDeadline_WhenCreatingLottery_ThenExceptionIsThrown() {
        when(representUserService.requireMember(OWNER_TOKEN)).thenReturn(ownerMember);

        assertThrows(IllegalArgumentException.class, () ->
                lotteryService.createLottery(
                        OWNER_TOKEN, EVENT_ID, COMPANY_ID,
                        LocalDateTime.now().plusDays(2),
                        LocalDateTime.now().plusDays(1)));
    }

    @Test
    @DisplayName("Given non-existent company, when creating lottery, then exception is thrown")
    void givenNonExistentCompany_WhenCreatingLottery_ThenExceptionIsThrown() {
        when(representUserService.requireMember(OWNER_TOKEN)).thenReturn(ownerMember);
        when(companyRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                lotteryService.createLottery(
                        OWNER_TOKEN, EVENT_ID, 99,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2)));
    }

    // =========================================================================
    // UC II.3.6 — Register to Lottery
    // =========================================================================

    @Test
    @DisplayName("Given verified member, when registering to open lottery, then entry DTO is returned")
    void givenVerifiedMember_WhenRegisteringToOpenLottery_ThenEntryDTOIsReturned() {
        when(representUserService.requireMember(MEMBER_TOKEN)).thenReturn(regularMember);

        Lottery lottery = new Lottery(EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
        when(lotteryRepository.findById(lottery.getId())).thenReturn(Optional.of(lottery));

        LotteryEntryDTO entry = lotteryService.registerToLottery(MEMBER_TOKEN, lottery.getId());

        assertNotNull(entry);
        assertEquals("member-1", entry.getMemberId());
        assertFalse(entry.isWinner());
        assertNull(entry.getAccessCode());
        verify(lotteryRepository).save(any());
    }

    @Test
    @DisplayName("Given member already registered, when registering again, then exception is thrown")
    void givenMemberAlreadyRegistered_WhenRegisteringAgain_ThenExceptionIsThrown() {
        when(representUserService.requireMember(MEMBER_TOKEN)).thenReturn(regularMember);

        Lottery lottery = new Lottery(EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
        lottery.register("member-1");

        when(lotteryRepository.findById(lottery.getId())).thenReturn(Optional.of(lottery));

        assertThrows(RuntimeException.class,
                () -> lotteryService.registerToLottery(MEMBER_TOKEN, lottery.getId()));
    }

    @Test
    @DisplayName("Given non-existent lottery, when registering, then exception is thrown")
    void givenNonExistentLottery_WhenRegistering_ThenExceptionIsThrown() {
        when(representUserService.requireMember(MEMBER_TOKEN)).thenReturn(regularMember);
        when(lotteryRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> lotteryService.registerToLottery(MEMBER_TOKEN, UUID.randomUUID()));
    }

    // =========================================================================
    // UC II.3.6 — Draw Lottery
    // =========================================================================

    @Test
    @DisplayName("Given owner and entries, when drawing lottery, then winners are returned with access codes")
    void givenOwnerAndEntries_WhenDrawingLottery_ThenWinnersAreReturnedWithAccessCodes() {
        when(representUserService.requireMember(OWNER_TOKEN)).thenReturn(ownerMember);

        Lottery lottery = new Lottery(EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
        lottery.register("member-1");
        lottery.register("member-2");
        lottery.register("member-3");

        when(lotteryRepository.findById(lottery.getId())).thenReturn(Optional.of(lottery));

        List<LotteryEntryDTO> winners = lotteryService.drawLottery(OWNER_TOKEN, lottery.getId(), 2);

        assertEquals(2, winners.size());
        winners.forEach(w -> {
            assertTrue(w.isWinner());
            assertNotNull(w.getAccessCode());
            assertNotNull(w.getAccessCodeExpiresAt());
        });
        verify(lotteryRepository).save(any());
    }

    @Test
    @DisplayName("Given fewer entries than winners count, when drawing, then all entries win")
    void givenFewerEntriesThanWinnersCount_WhenDrawing_ThenAllEntriesWin() {
        when(representUserService.requireMember(OWNER_TOKEN)).thenReturn(ownerMember);

        Lottery lottery = new Lottery(EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
        lottery.register("member-1");

        when(lotteryRepository.findById(lottery.getId())).thenReturn(Optional.of(lottery));

        List<LotteryEntryDTO> winners = lotteryService.drawLottery(OWNER_TOKEN, lottery.getId(), 10);

        assertEquals(1, winners.size());
        assertTrue(winners.get(0).isWinner());
    }

    @Test
    @DisplayName("Given non-owner token, when drawing lottery, then exception is thrown")
    void givenNonOwnerToken_WhenDrawingLottery_ThenExceptionIsThrown() {
        when(representUserService.requireMember(MEMBER_TOKEN)).thenReturn(regularMember);

        Lottery lottery = new Lottery(EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
        lottery.register("member-1");

        when(lotteryRepository.findById(lottery.getId())).thenReturn(Optional.of(lottery));

        assertThrows(RuntimeException.class,
                () -> lotteryService.drawLottery(MEMBER_TOKEN, lottery.getId(), 1));

        verify(lotteryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given already drawn lottery, when drawing again, then exception is thrown")
    void givenAlreadyDrawnLottery_WhenDrawingAgain_ThenExceptionIsThrown() {
        when(representUserService.requireMember(OWNER_TOKEN)).thenReturn(ownerMember);

        Lottery lottery = new Lottery(EVENT_ID, COMPANY_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));
        lottery.register("member-1");
        lottery.draw(1);

        when(lotteryRepository.findById(lottery.getId())).thenReturn(Optional.of(lottery));

        assertThrows(IllegalStateException.class,
                () -> lotteryService.drawLottery(OWNER_TOKEN, lottery.getId(), 1));
    }

    @Test
    @DisplayName("Given non-existent lottery, when drawing, then exception is thrown")
    void givenNonExistentLottery_WhenDrawing_ThenExceptionIsThrown() {
        when(representUserService.requireMember(OWNER_TOKEN)).thenReturn(ownerMember);
        when(lotteryRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> lotteryService.drawLottery(OWNER_TOKEN, UUID.randomUUID(), 1));
    }
}