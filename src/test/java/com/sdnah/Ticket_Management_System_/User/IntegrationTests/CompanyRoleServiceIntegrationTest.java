package com.sdnah.Ticket_Management_System_.User.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Application_Layer.CompanyRoleService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.ManagerPermission;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.User.IntegrationTests.testconfig.TestConfig;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class CompanyRoleServiceIntegrationTest {

    private static final String COMPANY_ID = "company-1";
    private static final String OWNER_ID = "owner-1";
    private static final String TARGET_ID = "target-1";
    private static final String MANAGER_ID = "manager-1";
    private static final String OWNER_TOKEN = "owner-token";

    @Autowired
    private CompanyRoleService companyRoleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        Member owner = new Member(OWNER_ID, "ownerUser", "hash");
        owner.addCompanyRole(new CompanyRoleAssignment(
                COMPANY_ID,
                OWNER_ID,
                CompanyRoleType.OWNER,
                Set.of()));

        Member target = new Member(TARGET_ID, "targetUser", "hash");

        Member manager = new Member(MANAGER_ID, "managerUser", "hash");
        manager.addCompanyRole(new CompanyRoleAssignment(
                COMPANY_ID,
                OWNER_ID,
                CompanyRoleType.MANAGER,
                Set.of()));

        userRepository.save(owner);
        userRepository.save(target);
        userRepository.save(manager);

        tokenRepository.save(new AuthToken(
                OWNER_TOKEN,
                OWNER_ID,
                LocalDateTime.now().plusHours(2)));
    }

    @Test
    @DisplayName("Given owner token and target member, when assigning manager, then target becomes manager")
    void givenOwnerTokenAndTargetMember_WhenAssigningManager_ThenTargetBecomesManager() {
        // Act
        companyRoleService.assignManager(OWNER_TOKEN, COMPANY_ID, TARGET_ID);

        // Assert
        Member savedTarget = userRepository.findById(TARGET_ID).orElseThrow();
        assertTrue(savedTarget.isManagerInCompany(COMPANY_ID));
    }

    @Test
    @DisplayName("Given owner token and target member, when assigning owner, then target becomes owner")
    void givenOwnerTokenAndTargetMember_WhenAssigningOwner_ThenTargetBecomesOwner() {
        // Act
        companyRoleService.assignOwner(OWNER_TOKEN, COMPANY_ID, TARGET_ID);

        // Assert
        Member savedTarget = userRepository.findById(TARGET_ID).orElseThrow();
        assertTrue(savedTarget.isOwnerInCompany(COMPANY_ID));
    }

    @Test
    @DisplayName("Given owner token and manager member, when adding permission, then permission is saved")
    void givenOwnerTokenAndManagerMember_WhenAddingPermission_ThenPermissionIsSaved() {
        // Act
        companyRoleService.addManagerPermission(
                OWNER_TOKEN,
                COMPANY_ID,
                MANAGER_ID,
                ManagerPermission.ADD_POLICY);

        // Assert
        Member savedManager = userRepository.findById(MANAGER_ID).orElseThrow();

        assertTrue(savedManager.getRoleInCompany(COMPANY_ID)
                .orElseThrow()
                .hasPermission(ManagerPermission.ADD_POLICY));
    }

    @Test
    @DisplayName("Given owner token and manager with permission, when removing permission, then permission is removed")
    void givenOwnerTokenAndManagerWithPermission_WhenRemovingPermission_ThenPermissionIsRemoved() {
        // Arrange
        companyRoleService.addManagerPermission(
                OWNER_TOKEN,
                COMPANY_ID,
                MANAGER_ID,
                ManagerPermission.ADD_POLICY);

        // Act
        companyRoleService.removeManagerPermission(
                OWNER_TOKEN,
                COMPANY_ID,
                MANAGER_ID,
                ManagerPermission.ADD_POLICY);

        // Assert
        Member savedManager = userRepository.findById(MANAGER_ID).orElseThrow();

        assertFalse(savedManager.getRoleInCompany(COMPANY_ID)
                .orElseThrow()
                .hasPermission(ManagerPermission.ADD_POLICY));
    }

    @Test
    @DisplayName("Given owner token and owner target, when removing owner, then owner role is removed")
    void givenOwnerTokenAndOwnerTarget_WhenRemovingOwner_ThenOwnerRoleIsRemoved() {
        // Arrange
        companyRoleService.assignOwner(OWNER_TOKEN, COMPANY_ID, TARGET_ID);

        // Act
        companyRoleService.removeOwner(OWNER_TOKEN, COMPANY_ID, TARGET_ID);

        // Assert
        Member savedTarget = userRepository.findById(TARGET_ID).orElseThrow();
        assertFalse(savedTarget.isOwnerInCompany(COMPANY_ID));
    }

    @Test
    @DisplayName("Given manager member with permission, when checking permission, then true is returned")
    void givenManagerMemberWithPermission_WhenCheckingPermission_ThenTrueIsReturned() {
        // Arrange
        companyRoleService.addManagerPermission(
                OWNER_TOKEN,
                COMPANY_ID,
                MANAGER_ID,
                ManagerPermission.VIEW_PURCHASE_HISTORY);

        // Act
        boolean result = companyRoleService.hasManagerPermission(
                MANAGER_ID,
                COMPANY_ID,
                ManagerPermission.VIEW_PURCHASE_HISTORY);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given target already has manager role, when assigning owner, then already has role exception is thrown")
    void givenTargetAlreadyHasManagerRole_WhenAssigningOwner_ThenAlreadyHasRoleExceptionIsThrown() {
        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignOwner(OWNER_TOKEN, COMPANY_ID, MANAGER_ID));

        // Assert
        assertEquals("Target already has a role in this Company", ex.getMessage());
    }

    @Test
    @DisplayName("Given missing target member, when assigning manager, then target member not found exception is thrown")
    void givenMissingTargetMember_WhenAssigningManager_ThenTargetMemberNotFoundExceptionIsThrown() {
        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignManager(OWNER_TOKEN, COMPANY_ID, "missing-member"));

        // Assert
        assertEquals("Target member not found", ex.getMessage());
    }

    @Test
    @DisplayName("Given manager member without permission, when checking permission, then false is returned")
    void givenManagerMemberWithoutPermission_WhenCheckingPermission_ThenFalseIsReturned() {
        // Act
        boolean result = companyRoleService.hasManagerPermission(
                MANAGER_ID,
                COMPANY_ID,
                ManagerPermission.REMOVE_POLICY);

        // Assert
        assertFalse(result);
    }
}
