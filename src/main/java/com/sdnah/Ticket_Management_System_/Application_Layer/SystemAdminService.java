package com.sdnah.Ticket_Management_System_.Application_Layer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

@Service
public class SystemAdminService {
    private static final Logger logger = LoggerFactory.getLogger(SystemAdminService.class);

    private final UserRepository userRepository;
    private final SystemAdminRepository systemAdminRepository;
    private final UserService userService;
    private final KeyedLock keyedLock;

    private static final String LOCK_NS = "system-admin:member";

    public SystemAdminService(UserRepository userRepository,
            SystemAdminRepository systemAdminRepository,
            UserService userService,
            KeyedLock keyedLock) {
        this.userRepository = userRepository;
        this.systemAdminRepository = systemAdminRepository;
        this.userService = userService;
        this.keyedLock = keyedLock;
    }

    @Transactional
    public void assign_system_admin(String token, String target_member_id) {
        logger.info("Assign system admin requested for targetMemberId={}", target_member_id);

        Member adminActor = requireAdmin(token);

        keyedLock.runLocked(LOCK_NS, target_member_id, () -> {
            Member to_assign = userRepository.findByMemberId(target_member_id);
            if (to_assign == null) {
                throw new IllegalArgumentException("Target member not found");
            }
            if (is_admin(target_member_id)) {
                throw new IllegalArgumentException("Member is already an admin");
            }

            System_admin new_admin = new System_admin(to_assign, adminActor.getMemberId());

            userRepository.delete(to_assign);
            userRepository.flush();
            systemAdminRepository.save(new_admin);
        });
    }

    public Member requireAdmin(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }

        Member admin = userService.getMemberByToken(token);

        if (!admin.isSystemAdmin()) {
            throw new IllegalArgumentException("Only system admins can perform this action");
        }

        return admin;
    }

    private boolean is_admin(String memberId) {
        return systemAdminRepository.existsByMemberId(memberId);
    }

    public void close_company(String token, String company_id) {
        requireAdmin(token);
        if (company_id == null || company_id.isBlank()) {
            throw new IllegalArgumentException("Company id is required");
        }
        throw new UnsupportedOperationException(
                "Company lifecycle operations are not available: missing Company domain/repository implementation");
    }

    public void open_company(String token, String company_id) {
        requireAdmin(token);
        if (company_id == null || company_id.isBlank()) {
            throw new IllegalArgumentException("Company id is required");
        }
        throw new UnsupportedOperationException(
                "Company lifecycle operations are not available: missing Company domain/repository implementation");
    }
}