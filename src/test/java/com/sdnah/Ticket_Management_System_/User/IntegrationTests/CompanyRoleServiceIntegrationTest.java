package com.sdnah.Ticket_Management_System_.User.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Company.CompanyRoleService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.ManagerPermission;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.User.IntegrationTests.testconfig.TestConfig;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class CompanyRoleServiceIntegrationTest {

        private static final int COMPANY_ID = 1;
        private static final String OWNER_ID = "owner-1";
        private static final String TARGET_ID = "target-1";
        private static final String MANAGER_ID = "manager-1";
        private static final String OWNER_USERNAME = "ownerUser";
        @MockBean
        private com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService activeOrderService;
@MockBean
private com.sdnah.Ticket_Management_System_.Application_Layer.PolicyService policyService;
@MockBean
private com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository policyRepository;
        @MockBean
        private com.sdnah.Ticket_Management_System_.Application_Layer.Factory factory;
        @Autowired
        private CompanyRoleService companyRoleService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private AuthTokenService authTokenService;

        private String ownerToken;

        @BeforeEach
        void setUp() {
                userRepository.deleteAll();

                Member owner = new Member(OWNER_ID, OWNER_USERNAME, "hash");
                owner.setVerified(true);
                owner.addCompanyRole(new CompanyRoleAssignment(
                                COMPANY_ID,
                                OWNER_ID,
                                CompanyRoleType.OWNER,
                                Set.of()));

                Member target = new Member(TARGET_ID, "targetUser", "hash");
                target.setVerified(true);

                Member manager = new Member(MANAGER_ID, "managerUser", "hash");
                manager.setVerified(true);
                manager.addCompanyRole(new CompanyRoleAssignment(
                                COMPANY_ID,
                                OWNER_ID,
                                CompanyRoleType.MANAGER,
                                Set.of()));

                userRepository.save(owner);
                userRepository.save(target);
                userRepository.save(manager);

                ownerToken = authTokenService.generateToken(OWNER_USERNAME);
        }

        @Test
        @DisplayName("Given owner token and target member, when assigning manager, then target becomes manager")
        void givenOwnerTokenAndTargetMember_WhenAssigningManager_ThenTargetBecomesManager() {
                companyRoleService.assignManager(ownerToken, COMPANY_ID, TARGET_ID);

                Member savedTarget = userRepository.findById(TARGET_ID).orElseThrow();
                assertTrue(savedTarget.isManagerInCompany(COMPANY_ID));
        }

        @Test
        @DisplayName("Given owner token and target member, when assigning owner, then target becomes owner")
        void givenOwnerTokenAndTargetMember_WhenAssigningOwner_ThenTargetBecomesOwner() {
                companyRoleService.assignOwner(ownerToken, COMPANY_ID, TARGET_ID);

                Member savedTarget = userRepository.findById(TARGET_ID).orElseThrow();
                assertTrue(savedTarget.isOwnerInCompany(COMPANY_ID));
        }

        @Test
        @DisplayName("Given owner token and manager member, when adding permission, then permission is saved")
        void givenOwnerTokenAndManagerMember_WhenAddingPermission_ThenPermissionIsSaved() {
                companyRoleService.addManagerPermission(
                                ownerToken,
                                COMPANY_ID,
                                MANAGER_ID,
                                ManagerPermission.ADD_POLICY);

                Member savedManager = userRepository.findById(MANAGER_ID).orElseThrow();

                assertTrue(savedManager.getRoleInCompany(COMPANY_ID)
                                .orElseThrow()
                                .hasPermission(ManagerPermission.ADD_POLICY));
        }

        @Test
        @DisplayName("Given owner token and manager with permission, when removing permission, then permission is removed")
        void givenOwnerTokenAndManagerWithPermission_WhenRemovingPermission_ThenPermissionIsRemoved() {
                companyRoleService.addManagerPermission(
                                ownerToken,
                                COMPANY_ID,
                                MANAGER_ID,
                                ManagerPermission.ADD_POLICY);

                companyRoleService.removeManagerPermission(
                                ownerToken,
                                COMPANY_ID,
                                MANAGER_ID,
                                ManagerPermission.ADD_POLICY);

                Member savedManager = userRepository.findById(MANAGER_ID).orElseThrow();

                assertFalse(savedManager.getRoleInCompany(COMPANY_ID)
                                .orElseThrow()
                                .hasPermission(ManagerPermission.ADD_POLICY));
        }

        @Test
        @DisplayName("Given owner token and owner target, when removing owner, then owner role is removed")
        void givenOwnerTokenAndOwnerTarget_WhenRemovingOwner_ThenOwnerRoleIsRemoved() {
                companyRoleService.assignOwner(ownerToken, COMPANY_ID, TARGET_ID);

                companyRoleService.removeOwner(ownerToken, COMPANY_ID, TARGET_ID);

                Member savedTarget = userRepository.findById(TARGET_ID).orElseThrow();
                assertFalse(savedTarget.isOwnerInCompany(COMPANY_ID));
        }

        @Test
        @DisplayName("Given manager member with permission, when checking permission, then true is returned")
        void givenManagerMemberWithPermission_WhenCheckingPermission_ThenTrueIsReturned() {
                companyRoleService.addManagerPermission(
                                ownerToken,
                                COMPANY_ID,
                                MANAGER_ID,
                                ManagerPermission.VIEW_PURCHASE_HISTORY);

                boolean result = companyRoleService.hasManagerPermission(
                                MANAGER_ID,
                                COMPANY_ID,
                                ManagerPermission.VIEW_PURCHASE_HISTORY);

                assertTrue(result);
        }

        @Test
        @DisplayName("Given target already has role, when assigning owner, then already has role exception is thrown")
        void givenTargetAlreadyHasRole_WhenAssigningOwner_ThenAlreadyHasRoleExceptionIsThrown() {
                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> companyRoleService.assignOwner(ownerToken, COMPANY_ID, MANAGER_ID));

                assertEquals("Target already has a role in this Company", ex.getMessage());
        }

        @Test
        @DisplayName("Given missing target member, when assigning manager, then target not found exception is thrown")
        void givenMissingTargetMember_WhenAssigningManager_ThenTargetNotFoundExceptionIsThrown() {
                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> companyRoleService.assignManager(ownerToken, COMPANY_ID, "missing-member"));

                assertEquals("Target member not found", ex.getMessage());
        }
}