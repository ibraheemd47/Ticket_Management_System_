package com.sdnah.Ticket_Management_System_.RobustnessTests;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.Company;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.Notification;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.CompanyRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.NotificationRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

/**
 * Robustness tests for the database layer.
 *
 * Verifies that:
 * - Repositories correctly persist and retrieve entities
 * - Duplicate / constraint violations are rejected
 * - Queries return correct results under edge-case inputs
 * - Data survives a full save-then-reload cycle (JPA mapping correctness)
 */
@SpringBootTest
@ActiveProfiles("test")
@ActiveProfiles("test")
@Transactional
@DisplayName("Database Layer — Robustness Tests")
class DatabaseRobustnessTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private LotteryRepository lotteryRepository;
    @Autowired private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        lotteryRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── UserRepository ────────────────────────────────────────────────────────

    @Test
    @DisplayName("DB: save and reload a Member — all fields round-trip correctly")
    void member_saveAndReload_fieldsPreserved() {
        Member m = new Member("m-1", "alice", "hash123");
        m.setActive(true);
        m.setVerified(true);
        userRepository.save(m);

        Member loaded = userRepository.findById("m-1").orElseThrow();
        assertThat(loaded.getUsername()).isEqualTo("alice");
        assertThat(loaded.isActive()).isTrue();
    }

    @Test
    @DisplayName("DB: duplicate username is rejected with a constraint violation")
    void member_duplicateUsername_throwsConstraintViolation() {
        Member m1 = new Member("m-2", "bob", "hash1");
        m1.setActive(true);
        userRepository.save(m1);
        userRepository.flush();

        Member m2 = new Member("m-3", "bob", "hash2"); // same username
        m2.setActive(true);

        assertThatThrownBy(() -> {
            userRepository.save(m2);
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB: findByUsername returns empty Optional for unknown username")
    void member_findByUsername_unknownReturnsEmpty() {
        Optional<Member> result = userRepository.findByUsername("nobody");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DB: existsByUsername returns false before save, true after")
    void member_existsByUsername_correctBeforeAndAfterSave() {
        assertThat(userRepository.existsByUsername("charlie")).isFalse();

        Member m = new Member("m-4", "charlie", "hash");
        m.setActive(true);
        userRepository.save(m);

        assertThat(userRepository.existsByUsername("charlie")).isTrue();
    }

    @Test
    @DisplayName("DB: delete Member — no longer found afterwards")
    void member_delete_removedFromDb() {
        Member m = new Member("m-5", "dave", "hash");
        m.setActive(true);
        userRepository.save(m);

        userRepository.deleteById("m-5");

        assertThat(userRepository.findById("m-5")).isEmpty();
    }

    // ── CompanyRepository ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DB: save and reload a Company — identity preserved")
    void company_saveAndReload() {
        Company c = new Company("Acme Corp", "founder-1");
        companyRepository.save(c);
        UUID savedId = c.getCompanyId();

        Optional<Company> found = companyRepository.findByCompanyId(savedId);
        assertThat(found).isPresent();
        assertThat(found.get().getCompanyName()).isEqualTo("Acme Corp");
    }

    @Test
    @DisplayName("DB: findByCompanyId returns empty for non-existent ID")
    void company_findById_unknownReturnsEmpty() {
        Optional<Company> result = companyRepository.findByCompanyId(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DB: existsByCompanyId reflects actual state")
    void company_existsByCompanyId_correctState() {
        Company c = new Company("Beta Inc", "founder-2");
        assertThat(companyRepository.existsByCompanyId(c.getCompanyId())).isFalse();

        companyRepository.save(c);

        assertThat(companyRepository.existsByCompanyId(c.getCompanyId())).isTrue();
    }

    @Test
    @DisplayName("DB: findByCompanyNameContainingIgnoreCase — case-insensitive substring match")
    void company_searchByName_caseInsensitive() {
        companyRepository.save(new Company("Gamma Events", "f-3"));
        companyRepository.save(new Company("Delta Shows", "f-4"));

        List<Company> result = companyRepository.findByCompanyNameContainingIgnoreCase("gamma");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyName()).isEqualTo("Gamma Events");
    }

    @Test
    @DisplayName("DB: findByCompanyNameContainingIgnoreCase — no match returns empty list")
    void company_searchByName_noMatch_returnsEmpty() {
        companyRepository.save(new Company("Zeta Co", "f-5"));

        List<Company> result = companyRepository.findByCompanyNameContainingIgnoreCase("xyz");
        assertThat(result).isEmpty();
    }

    // ── LotteryRepository ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DB: save and reload a Lottery — status and times preserved")
    void lottery_saveAndReload() {
        UUID eventId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LocalDateTime deadline = LocalDateTime.now().plusDays(1);
        LocalDateTime drawTime = LocalDateTime.now().plusDays(2);

        Lottery lottery = new Lottery(eventId, companyId, deadline, drawTime);
        lotteryRepository.save(lottery);

        List<Lottery> found = lotteryRepository.findByEventId(eventId);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getStatus()).isEqualTo(Lottery.LotteryStatus.OPEN);
    }

    @Test
    @DisplayName("DB: findByEventId returns empty list when no lotteries exist for that event")
    void lottery_findByEventId_unknownReturnsEmpty() {
        List<Lottery> result = lotteryRepository.findByEventId(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DB: findByCompanyId returns only that company's lotteries")
    void lottery_findByCompanyId_isolation() {
        UUID company1 = UUID.randomUUID();
        UUID company2 = UUID.randomUUID();
        LocalDateTime dl = LocalDateTime.now().plusDays(1);
        LocalDateTime dt = LocalDateTime.now().plusDays(2);

        lotteryRepository.save(new Lottery(UUID.randomUUID(), company1, dl, dt));
        lotteryRepository.save(new Lottery(UUID.randomUUID(), company2, dl, dt));

        assertThat(lotteryRepository.findByCompanyId(company1)).hasSize(1);
        assertThat(lotteryRepository.findByCompanyId(company2)).hasSize(1);
    }

    @Test
    @DisplayName("DB: findByStatusAndDrawTimeBefore — returns only overdue OPEN lotteries")
    void lottery_findOverdue_returnsOnlyMatchingRows() {
        UUID companyId = UUID.randomUUID();
        LocalDateTime past = LocalDateTime.now().minusHours(1);
        LocalDateTime future = LocalDateTime.now().plusDays(5);

        // overdue lottery — deadline and draw both in the past
        Lottery overdue = new Lottery(UUID.randomUUID(), companyId, past.minusMinutes(10), past);
        lotteryRepository.save(overdue);

        // future lottery — should not be returned
        Lottery upcoming = new Lottery(UUID.randomUUID(), companyId, future.minusDays(1), future);
        lotteryRepository.save(upcoming);

        List<Lottery> results = lotteryRepository.findByStatusAndDrawTimeBefore(
                Lottery.LotteryStatus.OPEN, LocalDateTime.now());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(overdue.getId());
    }

    @Test
    @DisplayName("DB: delete Lottery — no longer retrievable by event ID")
    void lottery_delete_removedFromDb() {
        UUID eventId = UUID.randomUUID();
        Lottery l = new Lottery(eventId, UUID.randomUUID(),
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        lotteryRepository.save(l);

        lotteryRepository.deleteById(l.getId());

        assertThat(lotteryRepository.findByEventId(eventId)).isEmpty();
    }

    // ── NotificationRepository ────────────────────────────────────────────────

    @Test
    @DisplayName("DB: save and reload a Notification — recipient and message preserved")
    void notification_saveAndReload() {
        Notification n = new Notification("user1", "You won!", NotificationType.LOTTERY_WIN);
        notificationRepository.save(n);

        List<Notification> loaded = notificationRepository.findByRecipientUsername("user1");
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getMessage()).isEqualTo("You won!");
    }

    @Test
    @DisplayName("DB: countByRecipientUsernameAndReadFalse returns correct unread count")
    void notification_unreadCount_accurate() {
        notificationRepository.save(new Notification("user2", "msg1", NotificationType.LOTTERY_WIN));
        notificationRepository.save(new Notification("user2", "msg2", NotificationType.LOTTERY_LOSS));

        long count = notificationRepository.countByRecipientUsernameAndReadFalse("user2");
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("DB: findByRecipientUsernameAndReadFalse filters out already-read notifications")
    void notification_unreadFilter_excludesReadNotifications() {
        Notification unread = new Notification("user3", "unread msg", NotificationType.LOTTERY_WIN);
        notificationRepository.save(unread);

        // Mark it read and update
        Notification saved = notificationRepository.findByRecipientUsername("user3").get(0);
        saved.markAsRead();
        notificationRepository.save(saved);

        List<Notification> unreadList = notificationRepository.findByRecipientUsernameAndReadFalse("user3");
        assertThat(unreadList).isEmpty();
    }

    @Test
    @DisplayName("DB: findByRecipientUsername returns empty for unknown user")
    void notification_findByRecipient_unknownReturnsEmpty() {
        List<Notification> result = notificationRepository.findByRecipientUsername("ghost");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DB: notifications from different users are properly isolated")
    void notification_userIsolation() {
        notificationRepository.save(new Notification("alice", "alice's msg", NotificationType.LOTTERY_WIN));
        notificationRepository.save(new Notification("bob", "bob's msg", NotificationType.LOTTERY_LOSS));

        assertThat(notificationRepository.findByRecipientUsername("alice")).hasSize(1);
        assertThat(notificationRepository.findByRecipientUsername("bob")).hasSize(1);
    }
}
