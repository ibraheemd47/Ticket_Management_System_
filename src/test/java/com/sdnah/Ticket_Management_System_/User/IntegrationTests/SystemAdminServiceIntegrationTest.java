package com.sdnah.Ticket_Management_System_.User.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.UserRole;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SystemAdminServiceIntegrationTest {

    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_USERNAME = "adminUser";
    private static final String TARGET_ID = "member-1";

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private SystemAdminRepository systemAdminRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        systemAdminRepository.deleteAll();
        userRepository.deleteAll();

        Member baseAdmin = new Member(ADMIN_ID, ADMIN_USERNAME, "hash");
        System_admin admin = new System_admin(baseAdmin, "System");

        Member targetMember = new Member(TARGET_ID, "targetUser", "hash");

        systemAdminRepository.save(admin);
        userRepository.save(targetMember);

        // JWT replaces the old DB-backed AuthToken; resolution happens by username.
        adminToken = authTokenService.generateToken(ADMIN_USERNAME);
    }

    @Test
    @DisplayName("Given valid admin token and target member, when assigning system admin, then admin is saved")
    void givenValidAdminTokenAndTargetMember_WhenAssigningSystemAdmin_ThenAdminIsSaved() {
        // Act
        systemAdminService.assign_system_admin(adminToken, TARGET_ID);

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

        String regularToken = authTokenService.generateToken("regularUser");

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(regularToken, TARGET_ID));

        // Assert
        assertEquals("Only system admins can perform this action", ex.getMessage());
    }

    @Test
    @DisplayName("Given missing target member, when assigning system admin, then member not found exception is thrown")
    void givenMissingTargetMember_WhenAssigningSystemAdmin_ThenMemberNotFoundExceptionIsThrown() {
        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(adminToken, "missing-member"));

        // Assert
        assertEquals("Target member not found", ex.getMessage());
    }
}
