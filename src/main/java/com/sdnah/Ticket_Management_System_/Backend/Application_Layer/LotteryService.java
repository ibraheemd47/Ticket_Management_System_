package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryEntryDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.LotteryAuthDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.LotteryEntry;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;

@Service
public class LotteryService {

    private static final Logger logger = LoggerFactory.getLogger(LotteryService.class);
    private static final String LOCK_NS = "lottery:entry";

    private final LotteryRepository lotteryRepository;
    private final CompanyRepository companyRepository;
    private final IrepresnteUserService representUserService;
    private final KeyedLock keyedLock;
    private final LotteryAuthDomainService lotteryAuth;

    public LotteryService(LotteryRepository lotteryRepository,
                          CompanyRepository companyRepository,
                          IrepresnteUserService representUserService,
                          KeyedLock keyedLock) {
        this.lotteryRepository  = lotteryRepository;
        this.companyRepository  = companyRepository;
        this.representUserService = representUserService;
        this.keyedLock          = keyedLock;
        this.lotteryAuth        = new LotteryAuthDomainService();
    }

    // =========================================================================
    // UC II.3.6 — CREATE LOTTERY - COMPANY OWNER ONLY
    // =========================================================================
    @Transactional
    public LotteryDTO createLottery(String actorToken, UUID eventId, int companyId,
                                    LocalDateTime registrationDeadline,
                                    LocalDateTime drawTime) {
        logger.info("Creating lottery for eventId={}, companyId={}", eventId, companyId);

        Member actor = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);
        lotteryAuth.assertCanCreateLottery(actor, company);

        Lottery lottery = new Lottery(eventId, companyId, registrationDeadline, drawTime);
        lotteryRepository.save(lottery);

        logger.info("Lottery created with id={}", lottery.getId());
        return toDTO(lottery);
    }

    // =========================================================================
    // UC II.3.6 — REGISTER TO LOTTERY (VERIFIED MEMBER ONLY)
    // =========================================================================
    @Transactional
    public synchronized LotteryEntryDTO registerToLottery(String actorToken, UUID lotteryId) {
        Member member = getActorFromToken(actorToken);

        logger.info("Member {} registering to lottery {}", member.getMemberId(), lotteryId);

        return keyedLock.callLocked(LOCK_NS, lotteryId.toString(), () -> {
            Lottery lottery = lotteryRepository.findById(lotteryId)
                    .orElseThrow(() -> new IllegalArgumentException("Lottery not found: " + lotteryId));

            lotteryAuth.assertCanRegisterToLottery(member, lottery);

            LotteryEntry entry = lottery.register(member.getMemberId());
            lotteryRepository.save(lottery);

            logger.info("Member {} registered to lottery {}", member.getMemberId(), lotteryId);
            return toEntryDTO(entry);
        });
    }

    // =========================================================================
    // UC II.3.6 — DRAW LOTTERY (COMPANY OWNER ONLY)
    // =========================================================================
    @Transactional
    public List<LotteryEntryDTO> drawLottery(String actorToken, UUID lotteryId, int winnersCount) {
        Member actor = getActorFromToken(actorToken);

        logger.info("Drawing lottery {}, winnersCount={}", lotteryId, winnersCount);

        return keyedLock.callLocked(LOCK_NS, lotteryId.toString(), () -> {
            Lottery lottery = lotteryRepository.findById(lotteryId)
                    .orElseThrow(() -> new IllegalArgumentException("Lottery not found: " + lotteryId));

            Company company = getCompanyOrThrow(lottery.getCompanyId());
            lotteryAuth.assertCanDrawLottery(actor, company);

            List<LotteryEntry> winners = lottery.draw(winnersCount);
            lotteryRepository.save(lottery);

            logger.info("Lottery {} drawn, winners={}", lotteryId, winners.size());
            return winners.stream().map(this::toEntryDTO).collect(Collectors.toList());
        });
    }

    // =========================================================================
    // Additional getters
    // =========================================================================
    public LotteryDTO getLottery(UUID lotteryId) {
        Lottery lottery = lotteryRepository.findById(lotteryId)
                .orElseThrow(() -> new IllegalArgumentException("Lottery not found: " + lotteryId));
        return toDTO(lottery);
    }

    public List<LotteryDTO> getLotteriesByEvent(UUID eventId) {
        return lotteryRepository.findByEventId(eventId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================
    private Member getActorFromToken(String actorToken) {
        if (actorToken == null || actorToken.isBlank()) {
            throw new SecurityException("Invalid token");
        }
        return representUserService.requireMember(actorToken);
    }

    private Company getCompanyOrThrow(int companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
    }

    // =========================================================================
    // Mappers
    // =========================================================================
    private LotteryDTO toDTO(Lottery lottery) {
        return new LotteryDTO(
                lottery.getId(),
                lottery.getEventId(),
                lottery.getCompanyId(),
                lottery.getRegistrationDeadline(),
                lottery.getDrawTime(),
                lottery.getStatus(),
                lottery.getEntries().size()
        );
    }

    private LotteryEntryDTO toEntryDTO(LotteryEntry entry) {
        return new LotteryEntryDTO(
                entry.getId(),
                entry.getLottery().getId(),
                entry.getMemberId(),
                entry.isWinner(),
                entry.getRegisteredAt(),
                entry.getAccessCode(),
                entry.getAccessCodeExpiresAt()
        );
    }
}