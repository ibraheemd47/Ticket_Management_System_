package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.HashSet;
import org.springframework.stereotype.Service;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;

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
}
