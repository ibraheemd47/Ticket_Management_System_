package com.sdnah.Ticket_Management_System_.User.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.UserRole;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

@ExtendWith(MockitoExtension.class)
class SystemAdminServiceAcceptanceTest {

    private static final String VALID_TOKEN = "token-123";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String ADMIN_ID = "admin-1";
    private static final String TARGET_MEMBER_ID = "member-1";

    @Mock
    private UserRepository userRepository;

    @Mock
    private SystemAdminRepository systemAdminRepository;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private SystemAdminService systemAdminService;

    private AuthToken validToken;
    private Member targetMember;

    @BeforeEach
    void setUp() {
        validToken = new AuthToken(VALID_TOKEN, ADMIN_ID, LocalDateTime.now().plusHours(2));
        targetMember = new Member(TARGET_MEMBER_ID, "mostafa", "hash");
    }

    @Test
    @DisplayName("Given valid admin token and target member, when assigning system admin, then new admin is saved")
    void givenValidAdminTokenAndTargetMember_WhenAssigningSystemAdmin_ThenNewAdminIsSaved() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);
        when(userRepository.findById(TARGET_MEMBER_ID)).thenReturn(Optional.of(targetMember));
        when(systemAdminRepository.existsById(TARGET_MEMBER_ID)).thenReturn(false);

        // Act
        systemAdminService.assign_system_admin(VALID_TOKEN, TARGET_MEMBER_ID);

        // Assert
        ArgumentCaptor<System_admin> adminCaptor = ArgumentCaptor.forClass(System_admin.class);
        verify(systemAdminRepository, times(1)).save(adminCaptor.capture());
        verify(userRepository, never()).save(any(Member.class));

        System_admin savedAdmin = adminCaptor.getValue();

        assertEquals(TARGET_MEMBER_ID, savedAdmin.getMemberId());
        assertEquals(targetMember.getUsername(), savedAdmin.getUsername());
        assertEquals(targetMember.getPasswordHash(), savedAdmin.getPasswordHash());
        assertEquals(UserRole.SYSTEM_ADMIN, savedAdmin.getRole());
        assertEquals(ADMIN_ID, savedAdmin.getWhoAssigned());
    }

    @Test
    @DisplayName("Given null token, when assigning system admin, then token required exception is thrown")
    void givenNullToken_WhenAssigningSystemAdmin_ThenTokenRequiredExceptionIsThrown() {
        // Arrange + Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(null, TARGET_MEMBER_ID));

        // Assert
        assertEquals("Token is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given invalid token, when assigning system admin, then invalid token exception is thrown")
    void givenInvalidToken_WhenAssigningSystemAdmin_ThenInvalidTokenExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(INVALID_TOKEN)).thenReturn(null);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(INVALID_TOKEN, TARGET_MEMBER_ID));

        // Assert
        assertEquals("Invalid token for admin", ex.getMessage());
    }

    @Test
    @DisplayName("Given non-admin token, when assigning system admin, then permission exception is thrown")
    void givenNonAdminToken_WhenAssigningSystemAdmin_ThenPermissionExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(false);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(VALID_TOKEN, TARGET_MEMBER_ID));

        // Assert
        assertEquals("Only system admins can assign new admins", ex.getMessage());
    }

    @Test
    @DisplayName("Given missing target member, when assigning system admin, then member not found exception is thrown")
    void givenMissingTargetMember_WhenAssigningSystemAdmin_ThenMemberNotFoundExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);
        when(userRepository.findById(TARGET_MEMBER_ID)).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(VALID_TOKEN, TARGET_MEMBER_ID));

        // Assert
        assertEquals("Member not found", ex.getMessage());
    }

    @Test
    @DisplayName("Given target member already admin, when assigning system admin, then already admin exception is thrown")
    void givenTargetMemberAlreadyAdmin_WhenAssigningSystemAdmin_ThenAlreadyAdminExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);
        when(userRepository.findById(TARGET_MEMBER_ID)).thenReturn(Optional.of(targetMember));
        when(systemAdminRepository.existsById(TARGET_MEMBER_ID)).thenReturn(true);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(VALID_TOKEN, TARGET_MEMBER_ID));

        // Assert
        assertEquals("Member is already an admin", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid admin token, when requiring admin, then admin member is returned")
    void givenValidAdminToken_WhenRequiringAdmin_ThenAdminMemberIsReturned() {
        // Arrange
        Member adminMember = new Member(ADMIN_ID, "adminUser", "hash");

        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminMember));

        // Act
        Member admin = systemAdminService.requireAdmin(VALID_TOKEN);

        // Assert
        assertNotNull(admin);
        assertEquals(ADMIN_ID, admin.getMemberId());
    }

    @Test
    @DisplayName("Given invalid token, when requiring admin, then invalid token exception is thrown")
    void givenInvalidToken_WhenRequiringAdmin_ThenInvalidTokenExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(INVALID_TOKEN)).thenReturn(null);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.requireAdmin(INVALID_TOKEN));

        // Assert
        assertEquals("Invalid token for admin", ex.getMessage());
    }

    @Test
    @DisplayName("Given non-admin token, when requiring admin, then permission exception is thrown")
    void givenNonAdminToken_WhenRequiringAdmin_ThenPermissionExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(false);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.requireAdmin(VALID_TOKEN));

        // Assert
        assertEquals("Only system admins can assign new admins", ex.getMessage());
    }

    // the next tests for close_company will be updated here when the method is
    // implemented in SystemAdminService
    @Test
    @DisplayName("Given valid admin token and valid company id, when closing company, then unsupported operation exception is thrown")
    void givenValidAdminTokenAndValidCompanyId_WhenClosingCompany_ThenUnsupportedOperationExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);

        // Act
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> systemAdminService.close_company(VALID_TOKEN, "company-1"));

        // Assert
        assertEquals(
                "Company lifecycle operations are not available: missing Company domain/repository implementation",
                ex.getMessage());
    }

    @Test
    @DisplayName("Given null company id, when closing company, then company id required exception is thrown")
    void givenNullCompanyId_WhenClosingCompany_ThenCompanyIdRequiredExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.close_company(VALID_TOKEN, null));

        // Assert
        assertEquals("Company id is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given blank company id, when closing company, then company id required exception is thrown")
    void givenBlankCompanyId_WhenClosingCompany_ThenCompanyIdRequiredExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.close_company(VALID_TOKEN, "   "));

        // Assert
        assertEquals("Company id is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given non-admin token, when closing company, then permission exception is thrown")
    void givenNonAdminToken_WhenClosingCompany_ThenPermissionExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(false);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.close_company(VALID_TOKEN, "company-1"));

        // Assert
        assertEquals("Only system admins can assign new admins", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid admin token and valid company id, when opening company, then unsupported operation exception is thrown")
    void givenValidAdminTokenAndValidCompanyId_WhenOpeningCompany_ThenUnsupportedOperationExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);

        // Act
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> systemAdminService.open_company(VALID_TOKEN, "company-1"));

        // Assert
        assertEquals(
                "Company lifecycle operations are not available: missing Company domain/repository implementation",
                ex.getMessage());
    }

    @Test
    @DisplayName("Given null company id, when opening company, then company id required exception is thrown")
    void givenNullCompanyId_WhenOpeningCompany_ThenCompanyIdRequiredExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.open_company(VALID_TOKEN, null));

        // Assert
        assertEquals("Company id is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given blank company id, when opening company, then company id required exception is thrown")
    void givenBlankCompanyId_WhenOpeningCompany_ThenCompanyIdRequiredExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(true);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.open_company(VALID_TOKEN, "   "));

        // Assert
        assertEquals("Company id is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given non-admin token, when opening company, then permission exception is thrown")
    void givenNonAdminToken_WhenOpeningCompany_ThenPermissionExceptionIsThrown() {
        // Arrange
        when(tokenRepository.findByTokenValue(VALID_TOKEN)).thenReturn(validToken);
        when(systemAdminRepository.existsById(ADMIN_ID)).thenReturn(false);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.open_company(VALID_TOKEN, "company-1"));

        // Assert
        assertEquals("Only system admins can assign new admins", ex.getMessage());
    }
}