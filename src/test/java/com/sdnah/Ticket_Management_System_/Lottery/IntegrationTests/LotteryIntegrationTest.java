package com.sdnah.Ticket_Management_System_.Lottery.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.LotteryService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryEntryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("LotteryService — Integration Tests")
public class LotteryIntegrationTest {

    @Autowired private LotteryService lotteryService;
    @Autowired private UserService userService;
    @Autowired private LotteryRepository lotteryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private company_managment_serivce companyService;

    private String ownerToken;
    private String memberToken;
    private UUID companyId;
    private UUID eventId;

    @BeforeEach
        void setUp() {
        eventId = UUID.randomUUID();

        String ownerId = userService.register(
                "owner_" + UUID.randomUUID(), "123456",
                "owner" + UUID.randomUUID() + "@test.com",
                "0501234567", 19 , VerificationMethod.EMAIL);

        String ownerUsername = userRepository.findById(ownerId)
                .orElseThrow()
                .getUsername();

        userService.verifyAccount(ownerUsername, "123456");
        ownerToken = userService.login(ownerUsername, "123456");

        String memberId = userService.register(
                "member_" + UUID.randomUUID(), "123456",
                "member" + UUID.randomUUID() + "@test.com",
                "0501234568", 19, VerificationMethod.EMAIL);

        String memberUsername = userRepository.findById(memberId)
                .orElseThrow()
                .getUsername();

        userService.verifyAccount(memberUsername, "123456");
        memberToken = userService.login(memberUsername, "123456");

        companyId = companyService.openCompany(ownerToken, "Test Company");
        }

    @Test
    @DisplayName("Given owner token, when creating lottery, then lottery is persisted in DB")
    void givenOwnerToken_WhenCreatingLottery_ThenLotteryIsPersistedInDB() {
        LotteryDTO dto = lotteryService.createLottery(
                ownerToken, eventId, companyId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));

        assertNotNull(dto.getId());
        assertTrue(lotteryRepository.findById(dto.getId()).isPresent());
    }

    @Test
    @DisplayName("Given verified member, when registering to lottery, then entry is persisted in DB")
    void givenVerifiedMember_WhenRegisteringToLottery_ThenEntryIsPersistedInDB() {
        LotteryDTO lotteryDTO = lotteryService.createLottery(
                ownerToken, eventId, companyId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));

        LotteryEntryDTO entry = lotteryService.registerToLottery(memberToken, lotteryDTO.getId());

        assertNotNull(entry.getId());
        assertFalse(entry.isWinner());

        var lottery = lotteryRepository.findById(lotteryDTO.getId()).orElseThrow();
        assertEquals(1, lottery.getEntries().size());
    }

    @Test
    @DisplayName("Given owner draws lottery, then winners have valid access codes")
    void givenOwnerDrawsLottery_ThenWinnersHaveValidAccessCodes() {
        LotteryDTO lotteryDTO = lotteryService.createLottery(
                ownerToken, eventId, companyId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));

        lotteryService.registerToLottery(memberToken, lotteryDTO.getId());

        List<LotteryEntryDTO> winners = lotteryService.drawLottery(ownerToken, lotteryDTO.getId(), 1);

        assertEquals(1, winners.size());
        assertTrue(winners.get(0).isWinner());
        assertNotNull(winners.get(0).getAccessCode());
        assertNotNull(winners.get(0).getAccessCodeExpiresAt());
    }

    @Test
    @DisplayName("Given member already registered, when registering again, then exception is thrown")
    void givenMemberAlreadyRegistered_WhenRegisteringAgain_ThenExceptionIsThrown() {
        LotteryDTO lotteryDTO = lotteryService.createLottery(
                ownerToken, eventId, companyId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));

        lotteryService.registerToLottery(memberToken, lotteryDTO.getId());

        assertThrows(RuntimeException.class,
                () -> lotteryService.registerToLottery(memberToken, lotteryDTO.getId()));
    }

    @Test
    @DisplayName("Given drawn lottery, when drawing again, then exception is thrown")
    void givenDrawnLottery_WhenDrawingAgain_ThenExceptionIsThrown() {
        LotteryDTO lotteryDTO = lotteryService.createLottery(
                ownerToken, eventId, companyId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2));

        lotteryService.registerToLottery(memberToken, lotteryDTO.getId());
        lotteryService.drawLottery(ownerToken, lotteryDTO.getId(), 1);

        assertThrows(IllegalStateException.class,
                () -> lotteryService.drawLottery(ownerToken, lotteryDTO.getId(), 1));
    }
}