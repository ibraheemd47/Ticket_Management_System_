package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.ManagerPermission;

@Service
public class CompanyRoleService {

    private final UserRepository userRepository;
    private final UserService userService;

    public CompanyRoleService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public void assignOwner(String actorToken, String CompanyId, String newOwnerId) {
        Member actor = userService.getMemberByToken(actorToken);

        if (!actor.isOwnerInCompany(CompanyId)) {
            throw new RuntimeException("Only Company owner can assign ownership");
        }

        Member target = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new RuntimeException("Target member not found"));

        if (target.isOwnerInCompany(CompanyId) || target.isManagerInCompany(CompanyId)) {
            throw new RuntimeException("Target already has a role in this Company");
        }

        CompanyRoleAssignment assignment = new CompanyRoleAssignment(CompanyId, actor.getMemberId(),
                CompanyRoleType.OWNER, new HashSet<>());

        target.addCompanyRole(assignment);
        userRepository.save(target);
    }

    public void assignManager(String actorToken, String CompanyId, String managerId) {
        Member actor = userService.getMemberByToken(actorToken);

        if (!actor.isOwnerInCompany(CompanyId)) {
            throw new RuntimeException("Only Company owner can assign manager");
        }

        Member target = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Target member not found"));

        if (target.isOwnerInCompany(CompanyId) || target.isManagerInCompany(CompanyId)) {
            throw new RuntimeException("Target already has a role in this Company");
        }

        CompanyRoleAssignment assignment = new CompanyRoleAssignment(CompanyId, actor.getMemberId(),
                CompanyRoleType.MANAGER, new HashSet<>());

        target.addCompanyRole(assignment);
        userRepository.save(target);
    }

    public void removeOwner(String actorToken, String CompanyId, String ownerId) {
        Member actor = userService.getMemberByToken(actorToken);

        if (!actor.isOwnerInCompany(CompanyId)) {
            throw new RuntimeException("Only Company owner can remove ownership");
        }

        Member target = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Target member not found"));

        if (!target.isOwnerInCompany(CompanyId)) {
            throw new RuntimeException("Target is not an owner in this Company");
        }

        Set<CompanyRoleAssignment> updatedRoles = new HashSet<>(target.getCompanyRoles());
        updatedRoles.removeIf(role -> role.getCompanyId().equals(CompanyId) && role.isOwner());

        target.setCompanyRoles(updatedRoles);
        userRepository.save(target);
    }

    public void addManagerPermission(String actorToken, String CompanyId, String managerId,
            ManagerPermission permission) {
        Member actor = userService.getMemberByToken(actorToken);

        if (!actor.isOwnerInCompany(CompanyId)) {
            throw new RuntimeException("Only Company owner can add manager permissions");
        }

        Member target = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Target member not found"));

        CompanyRoleAssignment managerRole = target.getRoleInCompany(CompanyId)
                .orElseThrow(() -> new RuntimeException("Target has no role in this Company"));

        if (!managerRole.isManager()) {
            throw new RuntimeException("Target is not a manager in this Company");
        }

        managerRole.addPermission(permission);
        userRepository.save(target);
    }

    public void removeManagerPermission(String actorToken, String CompanyId, String managerId,
            ManagerPermission permission) {
        Member actor = userService.getMemberByToken(actorToken);

        if (!actor.isOwnerInCompany(CompanyId)) {
            throw new RuntimeException("Only Company owner can remove manager permissions");
        }

        Member target = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Target member not found"));

        CompanyRoleAssignment managerRole = target.getRoleInCompany(CompanyId)
                .orElseThrow(() -> new RuntimeException("Target has no role in this Company"));

        if (!managerRole.isManager()) {
            throw new RuntimeException("Target is not a manager in this Company");
        }

        managerRole.removePermission(permission);
        userRepository.save(target);
    }

    public boolean hasManagerPermission(String managerId, String CompanyId, ManagerPermission permission) {
        Member target = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Target member not found"));

        CompanyRoleAssignment managerRole = target.getRoleInCompany(CompanyId)
                .orElseThrow(() -> new RuntimeException("Target has no role in this Company"));

        if (!managerRole.isManager()) {
            return false;
        }

        return managerRole.hasPermission(permission);
    }

    public Optional<CompanyRoleAssignment> getRoleInCompany(String memberId, String CompanyId) {
        Member target = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Target member not found"));
        return target.getRoleInCompany(CompanyId);
    }

    public boolean isOwnerInCompany(String memberId, String CompanyId) {
        Member target = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Target member not found"));
        return target.isOwnerInCompany(CompanyId);
    }

    public boolean isManagerInCompany(String memberId, String CompanyId) {
        Member target = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Target member not found"));
        return target.isManagerInCompany(CompanyId);
    }
}
