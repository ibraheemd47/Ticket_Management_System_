package com.sdnah.Ticket_Management_System_.User.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.UserRole;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SystemAdminServiceIntegrationTest {

    private static final String ADMIN_ID = "admin-1";
    private static final String TARGET_ID = "member-1";
    private static final String TOKEN_VALUE = "admin-token";

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private SystemAdminRepository systemAdminRepository;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        systemAdminRepository.deleteAll();
        userRepository.deleteAll();

        Member baseAdmin = new Member(ADMIN_ID, "adminUser", "hash");
        System_admin admin = new System_admin(baseAdmin, "System");

        Member targetMember = new Member(TARGET_ID, "targetUser", "hash");

        systemAdminRepository.save(admin);
        userRepository.save(targetMember);

        AuthToken token = new AuthToken(
                TOKEN_VALUE,
                ADMIN_ID,
                LocalDateTime.now().plusHours(2));

        tokenRepository.save(token);
    }

    @Test
    @DisplayName("Given valid admin token and target member, when assigning system admin, then admin is saved")
    void givenValidAdminTokenAndTargetMember_WhenAssigningSystemAdmin_ThenAdminIsSaved() {
        // Arrange

        // Act
        systemAdminService.assign_system_admin(TOKEN_VALUE, TARGET_ID);

        // Assert
        assertTrue(systemAdminRepository.existsById(TARGET_ID));

        System_admin savedAdmin = systemAdminRepository.findById(TARGET_ID).orElseThrow();
        assertEquals(TARGET_ID, savedAdmin.getMemberId());
        assertEquals(UserRole.SYSTEM_ADMIN, savedAdmin.getRole());
        assertEquals(ADMIN_ID, savedAdmin.getWhoAssigned());
    }

    @Test
    @DisplayName("Given non-admin token, when assigning system admin, then permission exception is thrown")
    void givenNonAdminToken_WhenAssigningSystemAdmin_ThenPermissionExceptionIsThrown() {
        // Arrange
        Member nonAdmin = new Member("regular-1", "regularUser", "hash");
        userRepository.save(nonAdmin);

        AuthToken token = new AuthToken(
                "regular-token",
                "regular-1",
                LocalDateTime.now().plusHours(2));
        tokenRepository.save(token);

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin("regular-token", TARGET_ID));

        // Assert
        assertEquals("Only system admins can assign new admins", ex.getMessage());
    }

    @Test
    @DisplayName("Given missing target member, when assigning system admin, then member not found exception is thrown")
    void givenMissingTargetMember_WhenAssigningSystemAdmin_ThenMemberNotFoundExceptionIsThrown() {
        // Arrange

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(TOKEN_VALUE, "missing-member"));

        // Assert
        assertEquals("Member not found", ex.getMessage());
    }
}
