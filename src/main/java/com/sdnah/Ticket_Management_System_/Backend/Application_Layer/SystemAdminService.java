package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

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

    //version 2 - member suspension/reactivation
    //use case ( II.6.7 )
    @Transactional
    public void suspendUser(String token, String targetMemberId, long durationHours) {
        requireAdmin(token);
        keyedLock.runLocked(LOCK_NS, targetMemberId, () -> {
            Member target = userRepository.findByMemberId(targetMemberId);
            if (target == null) throw new IllegalArgumentException("Member not found");
            if (target.isSystemAdmin()) throw new IllegalArgumentException("Cannot suspend a system admin");

            LocalDateTime until = LocalDateTime.now().plusHours(durationHours);
            target.suspend(until);
            userRepository.save(target);
            //todo: notify user of suspension and duration
            //notifier.notifyUser(targetMemberId,"Your account has been suspended until " + until);
        });
    }

    //use case ( II.6.7 )
    @Transactional
    public void suspendPermanently(String token, String targetMemberId) {
        requireAdmin(token);
        keyedLock.runLocked(LOCK_NS, targetMemberId, () -> {
            Member target = userRepository.findByMemberId(targetMemberId);
            if (target == null) throw new IllegalArgumentException("Member not found");
            if (target.isSystemAdmin()) throw new IllegalArgumentException("Cannot suspend a system admin");

            target.suspendPermanently();   
            userRepository.save(target);
            //todo: notify user of permanent suspension
            //notifier.notifyUser(targetMemberId,"Your account has been suspended permanently.");
        });
    }

    //use case ( II.6.8 )
    @Transactional
    public void unsuspendUser(String token, String targetMemberId) {
        requireAdmin(token);
        keyedLock.runLocked(LOCK_NS, targetMemberId, () -> {
            Member target = userRepository.findByMemberId(targetMemberId);
            if (target == null) throw new IllegalArgumentException("Member not found");
            if (!target.isSuspended()) throw new IllegalArgumentException("Member is not suspended");

            target.unsuspend();
            userRepository.save(target);
            //todo: notify user of reactivation
            //notifier.notifyUser(targetMemberId, "Your suspension has been lifted.");
        });
    }
}