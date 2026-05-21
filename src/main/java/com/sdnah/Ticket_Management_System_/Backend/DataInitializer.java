package com.sdnah.Ticket_Management_System_.Backend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Runs once on startup.
 * If no system admin exists in the database, creates one automatically
 * so the application is never locked out of admin functions.
 *
 * Default credentials  →  username: admin   password: admin123
 * Change these immediately after first login!
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // ── Change these defaults before deploying ────────────────────────────────
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    // ─────────────────────────────────────────────────────────────────────────

    private final SystemAdminRepository systemAdminRepository;
    private final UserRepository        userRepository;
    private final PasswordHasher        passwordHasher;

    public DataInitializer(SystemAdminRepository systemAdminRepository,
                           UserRepository        userRepository,
                           PasswordHasher        passwordHasher) {
        this.systemAdminRepository = systemAdminRepository;
        this.userRepository        = userRepository;
        this.passwordHasher        = passwordHasher;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (systemAdminRepository.count() > 0) {
            log.info("[DataInitializer] System admin already exists — skipping seed.");
            return;
        }

        log.warn("[DataInitializer] No system admin found — creating default admin account.");

        // 1. Build a regular Member
        String memberId    = UUID.randomUUID().toString();
        String passwordHash = passwordHasher.hash(ADMIN_PASSWORD);

        Member member = new Member(memberId, ADMIN_USERNAME, passwordHash);
        member.setActive(true);
        member.setVerified(true);   // skip the email-verification flow for the seed admin

        // 2. Promote directly to System_admin (no token required for seeding)
        System_admin admin = new System_admin(member);

        // 3. Persist
        systemAdminRepository.save(admin);

        log.warn("[DataInitializer] ✅ Default system admin created.");
        log.warn("[DataInitializer]    username : {}", ADMIN_USERNAME);
        log.warn("[DataInitializer]    password : {}", ADMIN_PASSWORD);
        log.warn("[DataInitializer]    memberId : {}", memberId);
        log.warn("[DataInitializer]    ⚠ Change the password after first login!");
    }
}
