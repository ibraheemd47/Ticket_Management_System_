package com.sdnah.Ticket_Management_System_.Application_Layer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAdminService {
    private static final Logger logger = LoggerFactory.getLogger(SystemAdminService.class);

    private final UserRepository userRepository;
    private final SystemAdminRepository systemAdminRepository;
    private final TokenRepository tokenRepository;
    private final KeyedLock keyedLock;

    private static final String LOCK_NS = "system-admin:member";

    public SystemAdminService(UserRepository userRepository, SystemAdminRepository systemAdminRepository,
            TokenRepository tokenRepository, KeyedLock keyedLock) {
        this.userRepository = userRepository;
        this.systemAdminRepository = systemAdminRepository;
        this.tokenRepository = tokenRepository;
        this.keyedLock = keyedLock;
    }

    @Transactional
    public void assign_system_admin(String token, String target_member_id) {
        logger.info("Assign system admin requested for targetMemberId={}", target_member_id);
        AuthToken user_token = requireAdminToken(token);

        keyedLock.runLocked(LOCK_NS, target_member_id, () -> {
            Member to_assign = userRepository.findById(target_member_id)
                    .orElseThrow(() -> new IllegalArgumentException("Member not found"));

            if (is_admin(target_member_id)) {
                logger.warn("Assign system admin rejected: member already admin, targetMemberId={}", target_member_id);
                throw new IllegalArgumentException("Member is already an admin");
            }

            // Promote: build the new System_admin from the existing Member's profile,
            // then delete the Member row and flush so the INSERT below uses the
            // SYSTEM_ADMIN discriminator without colliding on the primary key.
            System_admin new_admin = new System_admin(to_assign, user_token.getMemberId());
            userRepository.delete(to_assign);
            userRepository.flush();
            systemAdminRepository.save(new_admin);

            logger.info("System admin assigned successfully, targetMemberId={}, assignedBy={}",
                    target_member_id, user_token.getMemberId());
        });
    }

    public Member requireAdmin(String token) {
        AuthToken authToken = requireAdminToken(token);
        return userRepository.findById(authToken.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Admin member not found"));
    }

    private boolean is_admin(String memberId) {
        return systemAdminRepository.existsById(memberId);
    }

    private AuthToken requireAdminToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }

        AuthToken userToken = tokenRepository.findByTokenValue(token);
        if (userToken == null) {
            throw new IllegalArgumentException("Invalid token for admin");
        }

        if (!is_admin(userToken.getMemberId())) {
            throw new IllegalArgumentException("Only system admins can assign new admins");
        }

        return userToken;
    }

    public void close_company(String token, String company_id) {
        requireAdminToken(token);
        if (company_id == null || company_id.isBlank()) {
            throw new IllegalArgumentException("Company id is required");
        }

        logger.warn("close_company cannot be executed: missing company aggregate/repository, companyId={}", company_id);
        throw new UnsupportedOperationException(
                "Company lifecycle operations are not available: missing Company domain/repository implementation");
    }

    public void open_company(String token, String company_id) {
        requireAdminToken(token);
        if (company_id == null || company_id.isBlank()) {
            throw new IllegalArgumentException("Company id is required");
        }

        logger.warn("open_company cannot be executed: missing company aggregate/repository, companyId={}", company_id);
        throw new UnsupportedOperationException(
                "Company lifecycle operations are not available: missing Company domain/repository implementation");
    }

}
