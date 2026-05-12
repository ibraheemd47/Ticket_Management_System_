package com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.ManagerPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

@Service
public class CompanyRoleService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyRoleService.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final KeyedLock keyedLock;

    private static final String LOCK_NS = "company-role:member";

    public CompanyRoleService(UserRepository userRepository, UserService userService, KeyedLock keyedLock) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.keyedLock = keyedLock;
    }

    public void assignOwner(String actorToken, int companyId, String newOwnerId) {
        logger.info("Assign owner request received, companyId={}, newOwnerId={}", companyId, newOwnerId);

        Member actor = userService.getMemberByToken(actorToken);
        logger.debug("Actor resolved, actorId={}", actor.getMemberId());

        if (!actor.isOwnerInCompany(companyId)) {
            logger.warn("Assign owner rejected: actor is not an owner, actorId={}, companyId={}",
                    actor.getMemberId(), companyId);
            throw new RuntimeException("Only Company owner can assign ownership");
        }

        keyedLock.runLocked(LOCK_NS, newOwnerId, () -> {
            Member target = userRepository.findById(newOwnerId)
                    .orElseThrow(() -> {
                        logger.warn("Assign owner rejected: target member not found, newOwnerId={}", newOwnerId);
                        return new RuntimeException("Target member not found");
                    });

            if (target.isOwnerInCompany(companyId) || target.isManagerInCompany(companyId)) {
                logger.warn("Assign owner rejected: target already has a role, targetId={}, companyId={}",
                        newOwnerId, companyId);
                throw new RuntimeException("Target already has a role in this Company");
            }

            CompanyRoleAssignment assignment = new CompanyRoleAssignment(
                    companyId, actor.getMemberId(), CompanyRoleType.OWNER, new HashSet<>());

            target.addCompanyRole(assignment);
            userRepository.saveAndFlush(target);

            logger.info("Owner assigned successfully, actorId={}, targetId={}, companyId={}",
                    actor.getMemberId(), newOwnerId, companyId);
        });
    }

    public void assignManager(String actorToken, int companyId, String managerId) {
        logger.info("Assign manager request received, companyId={}, managerId={}", companyId, managerId);

        Member actor = userService.getMemberByToken(actorToken);
        logger.debug("Actor resolved, actorId={}", actor.getMemberId());

        if (!actor.isOwnerInCompany(companyId)) {
            logger.warn("Assign manager rejected: actor is not an owner, actorId={}, companyId={}",
                    actor.getMemberId(), companyId);
            throw new RuntimeException("Only Company owner can assign manager");
        }

        keyedLock.runLocked(LOCK_NS, managerId, () -> {
            Member target = userRepository.findById(managerId)
                    .orElseThrow(() -> {
                        logger.warn("Assign manager rejected: target member not found, managerId={}", managerId);
                        return new RuntimeException("Target member not found");
                    });

            if (target.isOwnerInCompany(companyId) || target.isManagerInCompany(companyId)) {
                logger.warn("Assign manager rejected: target already has a role, targetId={}, companyId={}",
                        managerId, companyId);
                throw new RuntimeException("Target already has a role in this Company");
            }

            CompanyRoleAssignment assignment = new CompanyRoleAssignment(
                    companyId, actor.getMemberId(), CompanyRoleType.MANAGER, new HashSet<>());

            target.addCompanyRole(assignment);
            userRepository.saveAndFlush(target);

            logger.info("Manager assigned successfully, actorId={}, targetId={}, companyId={}",
                    actor.getMemberId(), managerId, companyId);
        });
    }

    public void removeOwner(String actorToken, int companyId, String ownerId) {
        logger.info("Remove owner request received, companyId={}, ownerId={}", companyId, ownerId);

        Member actor = userService.getMemberByToken(actorToken);
        logger.debug("Actor resolved, actorId={}", actor.getMemberId());

        if (!actor.isOwnerInCompany(companyId)) {
            logger.warn("Remove owner rejected: actor is not an owner, actorId={}, companyId={}",
                    actor.getMemberId(), companyId);
            throw new RuntimeException("Only Company owner can remove ownership");
        }

        keyedLock.runLocked(LOCK_NS, ownerId, () -> {
            Member target = userRepository.findById(ownerId)
                    .orElseThrow(() -> {
                        logger.warn("Remove owner rejected: target member not found, ownerId={}", ownerId);
                        return new RuntimeException("Target member not found");
                    });

            if (!target.isOwnerInCompany(companyId)) {
                logger.warn("Remove owner rejected: target is not an owner, targetId={}, companyId={}",
                        ownerId, companyId);
                throw new RuntimeException("Target is not an owner in this Company");
            }

            Set<CompanyRoleAssignment> updatedRoles = new HashSet<>(target.getCompanyRoles());
            updatedRoles.removeIf(role -> role.getCompanyId() == companyId && role.isOwner());

            target.setCompanyRoles(updatedRoles);
            userRepository.saveAndFlush(target);

            logger.info("Owner removed successfully, actorId={}, targetId={}, companyId={}",
                    actor.getMemberId(), ownerId, companyId);
        });
    }

    public void addManagerPermission(String actorToken, int companyId, String managerId,
            ManagerPermission permission) {
        logger.info("Add manager permission request received, companyId={}, managerId={}, permission={}",
                companyId, managerId, permission);

        Member actor = userService.getMemberByToken(actorToken);
        logger.debug("Actor resolved, actorId={}", actor.getMemberId());

        if (!actor.isOwnerInCompany(companyId)) {
            logger.warn("Add permission rejected: actor is not an owner, actorId={}, companyId={}",
                    actor.getMemberId(), companyId);
            throw new RuntimeException("Only Company owner can add manager permissions");
        }

        keyedLock.runLocked(LOCK_NS, managerId, () -> {
            Member target = userRepository.findById(managerId)
                    .orElseThrow(() -> {
                        logger.warn("Add permission rejected: target member not found, managerId={}", managerId);
                        return new RuntimeException("Target member not found");
                    });

            CompanyRoleAssignment managerRole = target.getRoleInCompany(companyId)
                    .orElseThrow(() -> {
                        logger.warn("Add permission rejected: target has no role in company, targetId={}, companyId={}",
                                managerId, companyId);
                        return new RuntimeException("Target has no role in this Company");
                    });

            if (!managerRole.isManager()) {
                logger.warn("Add permission rejected: target is not a manager, targetId={}, companyId={}",
                        managerId, companyId);
                throw new RuntimeException("Target is not a manager in this Company");
            }

            managerRole.addPermission(permission);
            userRepository.saveAndFlush(target);

            logger.info("Permission added successfully, actorId={}, targetId={}, companyId={}, permission={}",
                    actor.getMemberId(), managerId, companyId, permission);
        });
    }

    public void removeManagerPermission(String actorToken, int companyId, String managerId,
            ManagerPermission permission) {
        logger.info("Remove manager permission request received, companyId={}, managerId={}, permission={}",
                companyId, managerId, permission);

        Member actor = userService.getMemberByToken(actorToken);
        logger.debug("Actor resolved, actorId={}", actor.getMemberId());

        if (!actor.isOwnerInCompany(companyId)) {
            logger.warn("Remove permission rejected: actor is not an owner, actorId={}, companyId={}",
                    actor.getMemberId(), companyId);
            throw new RuntimeException("Only Company owner can remove manager permissions");
        }

        keyedLock.runLocked(LOCK_NS, managerId, () -> {
            Member target = userRepository.findById(managerId)
                    .orElseThrow(() -> {
                        logger.warn("Remove permission rejected: target member not found, managerId={}", managerId);
                        return new RuntimeException("Target member not found");
                    });

            CompanyRoleAssignment managerRole = target.getRoleInCompany(companyId)
                    .orElseThrow(() -> {
                        logger.warn(
                                "Remove permission rejected: target has no role in company, targetId={}, companyId={}",
                                managerId, companyId);
                        return new RuntimeException("Target has no role in this Company");
                    });

            if (!managerRole.isManager()) {
                logger.warn("Remove permission rejected: target is not a manager, targetId={}, companyId={}",
                        managerId, companyId);
                throw new RuntimeException("Target is not a manager in this Company");
            }

            managerRole.removePermission(permission);
            userRepository.saveAndFlush(target);

            logger.info("Permission removed successfully, actorId={}, targetId={}, companyId={}, permission={}",
                    actor.getMemberId(), managerId, companyId, permission);
        });
    }

    public boolean hasManagerPermission(String managerId, int companyId, ManagerPermission permission) {
        logger.debug("Check manager permission, managerId={}, companyId={}, permission={}",
                managerId, companyId, permission);

        Member target = userRepository.findById(managerId)
                .orElseThrow(() -> {
                    logger.warn("Permission check failed: member not found, managerId={}", managerId);
                    return new RuntimeException("Target member not found");
                });

        CompanyRoleAssignment managerRole = target.getRoleInCompany(companyId)
                .orElseThrow(() -> {
                    logger.warn("Permission check failed: no role in company, managerId={}, companyId={}",
                            managerId, companyId);
                    return new RuntimeException("Target has no role in this Company");
                });

        if (!managerRole.isManager()) {
            logger.debug("Permission check result=false: target is not a manager, managerId={}, companyId={}",
                    managerId, companyId);
            return false;
        }

        boolean result = managerRole.hasPermission(permission);
        logger.debug("Permission check result={}, managerId={}, companyId={}, permission={}",
                result, managerId, companyId, permission);
        return result;
    }

    public Optional<CompanyRoleAssignment> getRoleInCompany(String memberId, int companyId) {
        logger.debug("Get role in company, memberId={}, companyId={}", memberId, companyId);

        Member target = userRepository.findById(memberId)
                .orElseThrow(() -> {
                    logger.warn("Get role failed: member not found, memberId={}", memberId);
                    return new RuntimeException("Target member not found");
                });

        Optional<CompanyRoleAssignment> role = target.getRoleInCompany(companyId);
        logger.debug("Get role result={}, memberId={}, companyId={}",
                role.isPresent() ? role.get().getRoleType() : "none", memberId, companyId);
        return role;
    }

    public boolean isOwnerInCompany(String memberId, int companyId) {
        logger.debug("Check isOwner, memberId={}, companyId={}", memberId, companyId);

        Member target = userRepository.findById(memberId)
                .orElseThrow(() -> {
                    logger.warn("isOwner check failed: member not found, memberId={}", memberId);
                    return new RuntimeException("Target member not found");
                });

        boolean result = target.isOwnerInCompany(companyId);
        logger.debug("isOwner result={}, memberId={}, companyId={}", result, memberId, companyId);
        return result;
    }

    public boolean isManagerInCompany(String memberId, int companyId) {
        logger.debug("Check isManager, memberId={}, companyId={}", memberId, companyId);

        Member target = userRepository.findById(memberId)
                .orElseThrow(() -> {
                    logger.warn("isManager check failed: member not found, memberId={}", memberId);
                    return new RuntimeException("Target member not found");
                });

        boolean result = target.isManagerInCompany(companyId);
        logger.debug("isManager result={}, memberId={}, companyId={}", result, memberId, companyId);
        return result;
    }
}