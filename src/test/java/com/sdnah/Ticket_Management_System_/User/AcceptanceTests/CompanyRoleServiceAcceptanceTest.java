package com.sdnah.Ticket_Management_System_.User.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Company.CompanyRoleService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleAssignment;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.CompanyRoleType;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.ManagerPermission;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

@ExtendWith(MockitoExtension.class)
class CompanyRoleServiceAcceptanceTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String COMPANY_ID = "company-1";
    private static final String OWNER_ID = "owner-1";
    private static final String TARGET_ID = "target-1";
    private static final String MANAGER_ID = "manager-1";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Spy
    private KeyedLock keyedLock = new KeyedLock();

    @InjectMocks
    private CompanyRoleService companyRoleService;

    private Member actorOwner;
    private Member actorNonOwner;
    private Member targetMember;
    private Member targetManager;
    private Member targetOwner;

    @BeforeEach
    void setUp() {
        actorOwner = new Member(OWNER_ID, "ownerUser", "hash");
        actorOwner.addCompanyRole(new CompanyRoleAssignment(
                COMPANY_ID, OWNER_ID, CompanyRoleType.OWNER, Set.of()));

        actorNonOwner = new Member("regular-1", "regularUser", "hash");

        targetMember = new Member(TARGET_ID, "targetUser", "hash");

        targetManager = new Member(MANAGER_ID, "managerUser", "hash");
        targetManager.addCompanyRole(new CompanyRoleAssignment(
                COMPANY_ID, OWNER_ID, CompanyRoleType.MANAGER, Set.of()));

        targetOwner = new Member("owner-2", "owner2", "hash");
        targetOwner.addCompanyRole(new CompanyRoleAssignment(
                COMPANY_ID, OWNER_ID, CompanyRoleType.OWNER, Set.of()));
    }

    @Test
    @DisplayName("Given owner actor and target without role, when assigning owner, then target is saved as owner")
    void givenOwnerActorAndTargetWithoutRole_WhenAssigningOwner_ThenTargetIsSavedAsOwner() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetMember));

        // Act
        companyRoleService.assignOwner(VALID_TOKEN, COMPANY_ID, TARGET_ID);

        // Assert
        assertTrue(targetMember.isOwnerInCompany(COMPANY_ID));
        verify(userRepository, times(1)).save(targetMember);
    }

    @Test
    @DisplayName("Given non-owner actor, when assigning owner, then permission exception is thrown")
    void givenNonOwnerActor_WhenAssigningOwner_ThenPermissionExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorNonOwner);

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignOwner(VALID_TOKEN, COMPANY_ID, TARGET_ID));

        // Assert
        assertEquals("Only Company owner can assign ownership", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given missing target member, when assigning owner, then target not found exception is thrown")
    void givenMissingTargetMember_WhenAssigningOwner_ThenTargetNotFoundExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.empty());

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignOwner(VALID_TOKEN, COMPANY_ID, TARGET_ID));

        // Assert
        assertEquals("Target member not found", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given target already has company role, when assigning owner, then already has role exception is thrown")
    void givenTargetAlreadyHasCompanyRole_WhenAssigningOwner_ThenAlreadyHasRoleExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(targetManager));

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignOwner(VALID_TOKEN, COMPANY_ID, MANAGER_ID));

        // Assert
        assertEquals("Target already has a role in this Company", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given owner actor and target without role, when assigning manager, then target is saved as manager")
    void givenOwnerActorAndTargetWithoutRole_WhenAssigningManager_ThenTargetIsSavedAsManager() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetMember));

        // Act
        companyRoleService.assignManager(VALID_TOKEN, COMPANY_ID, TARGET_ID);

        // Assert
        assertTrue(targetMember.isManagerInCompany(COMPANY_ID));
        verify(userRepository, times(1)).save(targetMember);
    }

    @Test
    @DisplayName("Given non-owner actor, when assigning manager, then permission exception is thrown")
    void givenNonOwnerActor_WhenAssigningManager_ThenPermissionExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorNonOwner);

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignManager(VALID_TOKEN, COMPANY_ID, TARGET_ID));

        // Assert
        assertEquals("Only Company owner can assign manager", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given target already has company role, when assigning manager, then already has role exception is thrown")
    void givenTargetAlreadyHasCompanyRole_WhenAssigningManager_ThenAlreadyHasRoleExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(targetManager));

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.assignManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID));

        // Assert
        assertEquals("Target already has a role in this Company", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given owner actor and owner target, when removing owner, then owner role is removed and target is saved")
    void givenOwnerActorAndOwnerTarget_WhenRemovingOwner_ThenOwnerRoleIsRemovedAndTargetIsSaved() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById("owner-2")).thenReturn(Optional.of(targetOwner));

        // Act
        companyRoleService.removeOwner(VALID_TOKEN, COMPANY_ID, "owner-2");

        // Assert
        assertFalse(targetOwner.isOwnerInCompany(COMPANY_ID));
        verify(userRepository, times(1)).save(targetOwner);
    }

    @Test
    @DisplayName("Given non-owner actor, when removing owner, then permission exception is thrown")
    void givenNonOwnerActor_WhenRemovingOwner_ThenPermissionExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorNonOwner);

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.removeOwner(VALID_TOKEN, COMPANY_ID, "owner-2"));

        // Assert
        assertEquals("Only Company owner can remove ownership", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given target is not owner, when removing owner, then target not owner exception is thrown")
    void givenTargetIsNotOwner_WhenRemovingOwner_ThenTargetNotOwnerExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetMember));

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.removeOwner(VALID_TOKEN, COMPANY_ID, TARGET_ID));

        // Assert
        assertEquals("Target is not an owner in this Company", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given owner actor and manager target, when adding manager permission, then permission is added and target is saved")
    void givenOwnerActorAndManagerTarget_WhenAddingManagerPermission_ThenPermissionIsAddedAndTargetIsSaved() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(targetManager));

        // Act
        companyRoleService.addManagerPermission(
                VALID_TOKEN, COMPANY_ID, MANAGER_ID, ManagerPermission.ADD_POLICY);

        // Assert
        assertTrue(targetManager.getRoleInCompany(COMPANY_ID).get()
                .hasPermission(ManagerPermission.ADD_POLICY));
        verify(userRepository, times(1)).save(targetManager);
    }

    @Test
    @DisplayName("Given non-owner actor, when adding manager permission, then permission exception is thrown")
    void givenNonOwnerActor_WhenAddingManagerPermission_ThenPermissionExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorNonOwner);

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.addManagerPermission(
                        VALID_TOKEN, COMPANY_ID, MANAGER_ID, ManagerPermission.ADD_POLICY));

        // Assert
        assertEquals("Only Company owner can add manager permissions", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given target has no company role, when adding manager permission, then no role exception is thrown")
    void givenTargetHasNoCompanyRole_WhenAddingManagerPermission_ThenNoRoleExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetMember));

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.addManagerPermission(
                        VALID_TOKEN, COMPANY_ID, TARGET_ID, ManagerPermission.ADD_POLICY));

        // Assert
        assertEquals("Target has no role in this Company", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given target is owner not manager, when adding manager permission, then not manager exception is thrown")
    void givenTargetIsOwnerNotManager_WhenAddingManagerPermission_ThenNotManagerExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById("owner-2")).thenReturn(Optional.of(targetOwner));

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.addManagerPermission(
                        VALID_TOKEN, COMPANY_ID, "owner-2", ManagerPermission.ADD_POLICY));

        // Assert
        assertEquals("Target is not a manager in this Company", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given manager has permission, when removing manager permission, then permission is removed and target is saved")
    void givenManagerHasPermission_WhenRemovingManagerPermission_ThenPermissionIsRemovedAndTargetIsSaved() {
        // Arrange
        CompanyRoleAssignment managerRole = targetManager.getRoleInCompany(COMPANY_ID).get();
        managerRole.addPermission(ManagerPermission.ADD_POLICY);

        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorOwner);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(targetManager));

        // Act
        companyRoleService.removeManagerPermission(
                VALID_TOKEN, COMPANY_ID, MANAGER_ID, ManagerPermission.ADD_POLICY);

        // Assert
        assertFalse(managerRole.hasPermission(ManagerPermission.ADD_POLICY));
        verify(userRepository, times(1)).save(targetManager);
    }

    @Test
    @DisplayName("Given non-owner actor, when removing manager permission, then permission exception is thrown")
    void givenNonOwnerActor_WhenRemovingManagerPermission_ThenPermissionExceptionIsThrown() {
        // Arrange
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(actorNonOwner);

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.removeManagerPermission(
                        VALID_TOKEN, COMPANY_ID, MANAGER_ID, ManagerPermission.ADD_POLICY));

        // Assert
        assertEquals("Only Company owner can remove manager permissions", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given manager has permission, when checking permission, then true is returned")
    void givenManagerHasPermission_WhenCheckingPermission_ThenTrueIsReturned() {
        // Arrange
        targetManager.getRoleInCompany(COMPANY_ID).get()
                .addPermission(ManagerPermission.VIEW_PURCHASE_HISTORY);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(targetManager));

        // Act
        boolean result = companyRoleService.hasManagerPermission(
                MANAGER_ID, COMPANY_ID, ManagerPermission.VIEW_PURCHASE_HISTORY);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given manager does not have permission, when checking permission, then false is returned")
    void givenManagerDoesNotHavePermission_WhenCheckingPermission_ThenFalseIsReturned() {
        // Arrange
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(targetManager));

        // Act
        boolean result = companyRoleService.hasManagerPermission(
                MANAGER_ID, COMPANY_ID, ManagerPermission.VIEW_PURCHASE_HISTORY);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given target is owner not manager, when checking manager permission, then false is returned")
    void givenTargetIsOwnerNotManager_WhenCheckingManagerPermission_ThenFalseIsReturned() {
        // Arrange
        when(userRepository.findById("owner-2")).thenReturn(Optional.of(targetOwner));

        // Act
        boolean result = companyRoleService.hasManagerPermission(
                "owner-2", COMPANY_ID, ManagerPermission.ADD_POLICY);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given member has role in company, when getting role, then role is returned")
    void givenMemberHasRoleInCompany_WhenGettingRole_ThenRoleIsReturned() {
        // Arrange
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(targetManager));

        // Act
        Optional<CompanyRoleAssignment> result = companyRoleService.getRoleInCompany(MANAGER_ID, COMPANY_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(CompanyRoleType.MANAGER, result.get().getRoleType());
    }

    @Test
    @DisplayName("Given member has no role in company, when getting role, then empty optional is returned")
    void givenMemberHasNoRoleInCompany_WhenGettingRole_ThenEmptyOptionalIsReturned() {
        // Arrange
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetMember));

        // Act
        Optional<CompanyRoleAssignment> result = companyRoleService.getRoleInCompany(TARGET_ID, COMPANY_ID);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Given owner member, when checking owner in company, then true is returned")
    void givenOwnerMember_WhenCheckingOwnerInCompany_ThenTrueIsReturned() {
        // Arrange
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(actorOwner));

        // Act
        boolean result = companyRoleService.isOwnerInCompany(OWNER_ID, COMPANY_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given regular member, when checking owner in company, then false is returned")
    void givenRegularMember_WhenCheckingOwnerInCompany_ThenFalseIsReturned() {
        // Arrange
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetMember));

        // Act
        boolean result = companyRoleService.isOwnerInCompany(TARGET_ID, COMPANY_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Given manager member, when checking manager in company, then true is returned")
    void givenManagerMember_WhenCheckingManagerInCompany_ThenTrueIsReturned() {
        // Arrange
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(targetManager));

        // Act
        boolean result = companyRoleService.isManagerInCompany(MANAGER_ID, COMPANY_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Given missing member, when checking manager in company, then target not found exception is thrown")
    void givenMissingMember_WhenCheckingManagerInCompany_ThenTargetNotFoundExceptionIsThrown() {
        // Arrange
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> companyRoleService.isManagerInCompany(MANAGER_ID, COMPANY_ID));

        // Assert
        assertEquals("Target member not found", ex.getMessage());
    }
}