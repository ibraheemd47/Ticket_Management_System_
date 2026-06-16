package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.LotteryEntryDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.LotteryAuthDomainService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.LotteryEntry;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy.SellingType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;


@Service
public class LotteryService {

    private static final Logger logger = LoggerFactory.getLogger(LotteryService.class);
    private static final String LOCK_NS = "lottery:entry";

    private final LotteryRepository lotteryRepository;
    private final CompanyRepository companyRepository;
    private final IrepresnteUserService representUserService;
    private final KeyedLock keyedLock;
    private final LotteryAuthDomainService lotteryAuth;
    private final NotificationService notificationService;


    @org.springframework.beans.factory.annotation.Autowired
    private com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository policyRepository;

    public LotteryService(LotteryRepository lotteryRepository,
                          CompanyRepository companyRepository,
                          IrepresnteUserService representUserService,
                          KeyedLock keyedLock,
                          NotificationService notificationService) 
    {
        if (lotteryRepository == null) throw new IllegalArgumentException("lotteryRepository cannot be null");
        if (companyRepository == null) throw new IllegalArgumentException("companyRepository cannot be null");
        if (representUserService == null) throw new IllegalArgumentException("representUserService cannot be null");
        if (keyedLock == null) throw new IllegalArgumentException("keyedLock cannot be null");
        if (notificationService == null) throw new IllegalArgumentException("notificationService cannot be null");
        this.lotteryRepository  = lotteryRepository;
        this.companyRepository  = companyRepository;
        this.representUserService = representUserService;
        this.keyedLock          = keyedLock;
        this.lotteryAuth        = new LotteryAuthDomainService();
        this.notificationService = notificationService;
    }

    // =========================================================================
    // UC II.3.6 — CREATE LOTTERY - COMPANY OWNER ONLY
    // =========================================================================
    
    @Transactional
    public LotteryDTO createLottery(String actorToken, UUID eventId, UUID companyId,
                                    LocalDateTime registrationDeadline,
                                    LocalDateTime drawTime) {
        logger.info("Creating lottery for eventId={}, companyId={}", eventId, companyId);

        // Object sp = policyRepository.findSellingPolicyByEventId(eventId);
        // com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy policy =
        //         (sp instanceof java.util.Optional<?> o)
        //                 ? (com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy) o.orElse(null)
        //                 : (com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy) sp;
        // if (policy == null|| policy.getType() != com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy.SellingType.LOTTERY) {
        //     throw new IllegalStateException(
        //             "Cannot create a lottery: the event's selling policy is not LOTTERY");
       // }

        Member actor = getActorFromToken(actorToken);
        Company company = getCompanyOrThrow(companyId);
        lotteryAuth.assertCanCreateLottery(actor, company);

        //new fix:
        SellingPolicy policy = findEventSellingPolicyOrNull(eventId);
        if (policy != null &&
                policy.getType() != SellingType.LOTTERY) {
            throw new IllegalStateException(
                    "Cannot create a lottery: the event's selling policy is not LOTTERY");
        }

        Lottery lottery = new Lottery(eventId, companyId, registrationDeadline, drawTime);
        lotteryRepository.save(lottery);

        logger.info("Lottery created with id={}", lottery.getId());
        return toDTO(lottery);
    }

    private SellingPolicy findEventSellingPolicyOrNull(UUID eventId) {
        if (policyRepository == null) {
            return null;
        }

        Object sp = policyRepository.findSellingPolicyByEventId(eventId);

        if (sp == null) {
            return null;
        }

        if (sp instanceof java.util.Optional<?> opt) {
            return (SellingPolicy)
                    opt.orElse(null);
        }

        return (SellingPolicy) sp;
        
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
        return drawLottery(actorToken, lotteryId, winnersCount, LocalDateTime.now().plusHours(24));
    }

    @Transactional
    public List<LotteryEntryDTO> drawLottery(String actorToken, UUID lotteryId, int winnersCount,
                                             LocalDateTime openSaleTime) {
        Member actor = getActorFromToken(actorToken);
        logger.info("Drawing lottery {}, winnersCount={}, openSaleTime={}", lotteryId, winnersCount, openSaleTime);

        return keyedLock.callLocked(LOCK_NS, lotteryId.toString(), () -> {
            Lottery lottery = lotteryRepository.findById(lotteryId)
                    .orElseThrow(() -> new IllegalArgumentException("Lottery not found: " + lotteryId));

            Company company = getCompanyOrThrow(lottery.getCompanyId());
            lotteryAuth.assertCanDrawLottery(actor, company);

            List<LotteryEntry> winners = lottery.draw(winnersCount, openSaleTime);
            lotteryRepository.save(lottery);

            notifyDrawResults(lottery);

            logger.info("Lottery {} drawn, winners={}", lotteryId, winners.size());
            return winners.stream().map(this::toEntryDTO).collect(Collectors.toList());
        });
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 5000)
    @Transactional
    public void runDueDraws() {
        List<Lottery> due = lotteryRepository.findByStatusAndDrawTimeBefore(
                Lottery.LotteryStatus.OPEN, LocalDateTime.now());
        //logger.info("RUN DUE DRAWS - found: {}", due.size());
        for (Lottery lottery : due) {
            keyedLock.callLocked(LOCK_NS, lottery.getId().toString(), () -> {
                try {
                    if (lottery.getEntries().isEmpty()) {
                        logger.info("Auto-draw skipped (no participants) lottery={}", lottery.getId());
                        return null;
                    }
                    // automatic draw: exactly 1 winner, public sale opens 24h later
                    lottery.draw(1, LocalDateTime.now().plusHours(24));
                    lotteryRepository.save(lottery);
                    notifyDrawResults(lottery);
                    logger.info("Auto-drew lottery {} (1 winner)", lottery.getId());
                } catch (Exception ex) {
                    logger.error("Auto-draw failed for lottery {}: {}", lottery.getId(), ex.getMessage());
                }
                return null;
            });
        }
    }
    // @Scheduled(fixedRate = 60000)   // check every minute
    // @Transactional
    // public void runDueDraws() {
    //     List<Lottery> due = lotteryRepository
    //             .findByStatusAndDrawTimeBefore(Lottery.LotteryStatus.OPEN, LocalDateTime.now());
    //     for (Lottery lottery : due) {
    //         keyedLock.callLocked(LOCK_NS, lottery.getId().toString(), () -> {
    //             try {
    //                 int winnersCount = lottery.getEntries().size();
    //                 if (winnersCount == 0) {
    //                     logger.info("Auto-draw skipped (no participants) lottery={}", lottery.getId());
    //                     return null;   // nothing to draw; leave it for the manager or close it
    //                 }
    //                 lottery.draw(winnersCount, java.time.Duration.ofHours(24));   // auto-draw default window
    //                 lotteryRepository.save(lottery);
    //                 logger.info("Auto-drew lottery {} with {} winner(s)", lottery.getId(), winnersCount);
    //             } catch (Exception ex) {
    //                 logger.error("Auto-draw failed for lottery {}: {}", lottery.getId(), ex.getMessage());
    //             }
    //             return null;
    //         });
    //     }
    // }

    
    // @Transactional
    // public List<LotteryEntryDTO> drawLottery(String actorToken, UUID lotteryId, int winnersCount) {
    //     return drawLottery(actorToken, lotteryId, winnersCount, java.time.Duration.ofHours(24));
    // }

    // @Transactional
    // public List<LotteryEntryDTO> drawLottery(String actorToken, UUID lotteryId, int winnersCount,
    //                                          java.time.Duration exclusiveWindow) {
    //     Member actor = getActorFromToken(actorToken);

    //     logger.info("Drawing lottery {}, winnersCount={}, window={}", lotteryId, winnersCount, exclusiveWindow);

    //     return keyedLock.callLocked(LOCK_NS, lotteryId.toString(), () -> {
    //         Lottery lottery = lotteryRepository.findById(lotteryId)
    //                 .orElseThrow(() -> new IllegalArgumentException("Lottery not found: " + lotteryId));

    //         Company company = getCompanyOrThrow(lottery.getCompanyId());
    //         lotteryAuth.assertCanDrawLottery(actor, company);

    //         List<LotteryEntry> winners = lottery.draw(winnersCount, exclusiveWindow);
    //         lotteryRepository.save(lottery);

    //         logger.info("Lottery {} drawn, winners={}", lotteryId, winners.size());
    //         return winners.stream().map(this::toEntryDTO).collect(Collectors.toList());
    //     });
    // }

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

    @Transactional(readOnly = true)
    public List<LotteryEntryDTO> getEntriesByLottery(UUID lotteryId) {
        Lottery lottery = lotteryRepository.findById(lotteryId)
                .orElseThrow(() -> new IllegalArgumentException("Lottery not found: " + lotteryId));
        return lottery.getEntries().stream()
                .map(this::toEntryDTO)
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

    private Company getCompanyOrThrow(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Company ID " + companyId + " not found."));
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
                lottery.getEntries().size() ,
                lottery.getOpenSaleTime()
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

    @Transactional
    public void redeemAccessCode(UUID eventId, String memberId) {
        lotteryRepository.findByEventId(eventId).stream()
                .flatMap(l -> l.getEntries().stream())
                .filter(e -> e.getMemberId().equals(memberId))
                .filter(LotteryEntry::isWinner)
                .filter(e -> e.getUsedAt() == null)
                .findFirst()
                .ifPresent(e -> {
                    e.markCodeUsed();
                    lotteryRepository.save(e.getLottery());
                });
    }

    private void notifyDrawResults(Lottery lottery) {
        String eventName = lottery.getEventId().toString();
        List<Notification> batch = new ArrayList<>();
        for (LotteryEntry e : lottery.getEntries()) {
            try {
                if (e.isWinner()) {
                    String msg = "🎉 You won the lottery for \"" + eventName
                            + "\"! Your access code: " + e.getAccessCode();
                    batch.add(new Notification(e.getMemberId(), msg, NotificationType.LOTTERY_WIN));
                } else {
                    String msg = "Better luck next time! You were not selected in the lottery for \""
                            + eventName + "\".";
                    batch.add(new Notification(e.getMemberId(), msg, NotificationType.LOTTERY_LOSS));
                }
            } catch (Exception ex) {
                logger.warn("Failed to build notification for member {} in lottery {}: {}",
                        e.getMemberId(), lottery.getId(), ex.getMessage());
            }
        }
        // Single batch INSERT instead of N individual saves
        notificationService.createNotificationsBatch(batch);
    }

    public boolean isWinnerWindowOpen(UUID eventId) {
    LocalDateTime now = LocalDateTime.now();

    return lotteryRepository.findByEventId(eventId).stream()
            .filter(l -> l.getStatus() == Lottery.LotteryStatus.DRAWN)
            .filter(l -> l.getOpenSaleTime() != null)
            .anyMatch(l -> now.isBefore(l.getOpenSaleTime()));
} 
}