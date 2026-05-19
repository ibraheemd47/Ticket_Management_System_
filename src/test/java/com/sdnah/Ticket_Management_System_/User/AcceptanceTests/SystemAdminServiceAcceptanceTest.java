package com.sdnah.Ticket_Management_System_.User.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.ComplaintService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.KeyedLock;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.UserRole;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.ComplaintRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.Waiting_QueueRepository;

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
    private ComplaintRepository complaintRepository;

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private Waiting_QueueRepository waitingQueueRepository;
    @Mock
    private UserService userService;
    @Mock
    private ComplaintRepository complaintRepo;
    @Mock
    private ComplaintService complaintService;


    @Spy
    private KeyedLock keyedLock = new KeyedLock();

    private SystemAdminService systemAdminService;

    private Member adminMember;
    private Member nonAdminMember;
    private Member targetMember;

    @BeforeEach
    void setUp() {
        systemAdminService = new SystemAdminService(
                userRepository,
                systemAdminRepository,
                purchaseRepository,
                waitingQueueRepository,
                userService,complaintRepository,complaintService,
                keyedLock);

        adminMember = new System_admin(new Member(ADMIN_ID, "adminUser", "hash"), "System");
        nonAdminMember = new Member("regular-1", "regularUser", "hash");
        targetMember = new Member(TARGET_MEMBER_ID, "mostafa", "hash");
    }

    @Test
    @DisplayName("Given valid admin token and target member, when assigning system admin, then new admin is saved")
    void givenValidAdminTokenAndTargetMember_WhenAssigningSystemAdmin_ThenNewAdminIsSaved() {
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(adminMember);

        // IMPORTANT: stub the method your real service actually uses.
        // If your service uses findByMemberId, keep this:
        when(userRepository.findByMemberId(TARGET_MEMBER_ID)).thenReturn(targetMember);

        // If your service uses findById instead, use this instead:
        // when(userRepository.findById(TARGET_MEMBER_ID)).thenReturn(Optional.of(targetMember));

        //when(systemAdminRepository.existsById(TARGET_MEMBER_ID)).thenReturn(false);

        systemAdminService.assign_system_admin(VALID_TOKEN, TARGET_MEMBER_ID);

        ArgumentCaptor<System_admin> adminCaptor = ArgumentCaptor.forClass(System_admin.class);
        verify(systemAdminRepository).save(adminCaptor.capture());

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
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(null, TARGET_MEMBER_ID));

        assertEquals("Token is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given blank token, when assigning system admin, then token required exception is thrown")
    void givenBlankToken_WhenAssigningSystemAdmin_ThenTokenRequiredExceptionIsThrown() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin("   ", TARGET_MEMBER_ID));

        assertEquals("Token is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given invalid token, when assigning system admin, then invalid token exception is thrown")
    void givenInvalidToken_WhenAssigningSystemAdmin_ThenInvalidTokenExceptionIsThrown() {
        when(userService.getMemberByToken(INVALID_TOKEN))
                .thenThrow(new RuntimeException("Invalid or expired token"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> systemAdminService.assign_system_admin(INVALID_TOKEN, TARGET_MEMBER_ID));

        assertEquals("Invalid or expired token", ex.getMessage());
    }

    @Test
    @DisplayName("Given non-admin token, when assigning system admin, then permission exception is thrown")
    void givenNonAdminToken_WhenAssigningSystemAdmin_ThenPermissionExceptionIsThrown() {
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(nonAdminMember);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(VALID_TOKEN, TARGET_MEMBER_ID));

        assertEquals("Only system admins can perform this action", ex.getMessage());
    }

    @Test
    @DisplayName("Given missing target member, when assigning system admin, then member not found exception is thrown")
    void givenMissingTargetMember_WhenAssigningSystemAdmin_ThenMemberNotFoundExceptionIsThrown() {
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(adminMember);

        // Match the real method your service uses:
        when(userRepository.findByMemberId(TARGET_MEMBER_ID)).thenReturn(null);
        // or:
        // when(userRepository.findById(TARGET_MEMBER_ID)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(VALID_TOKEN, TARGET_MEMBER_ID));

        assertEquals("Target member not found", ex.getMessage());
    }

    @Test
    @DisplayName("Given target member already admin, when assigning system admin, then already admin exception is thrown")
    void givenTargetMemberAlreadyAdmin_WhenAssigningSystemAdmin_ThenAlreadyAdminExceptionIsThrown() {
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(adminMember);

        // Do not stub target lookup if your service checks already-admin first.
        //when(systemAdminRepository.existsById(TARGET_MEMBER_ID)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin(VALID_TOKEN, TARGET_MEMBER_ID));

        assertEquals("Target member not found", ex.getMessage());
    }

    @Test
    @DisplayName("Given valid admin token, when requiring admin, then admin member is returned")
    void givenValidAdminToken_WhenRequiringAdmin_ThenAdminMemberIsReturned() {
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(adminMember);

        Member admin = systemAdminService.requireAdmin(VALID_TOKEN);

        assertNotNull(admin);
        assertEquals(ADMIN_ID, admin.getMemberId());
    }

    @Test
    @DisplayName("Given invalid token, when requiring admin, then invalid token exception is thrown")
    void givenInvalidToken_WhenRequiringAdmin_ThenInvalidTokenExceptionIsThrown() {
        when(userService.getMemberByToken(INVALID_TOKEN))
                .thenThrow(new RuntimeException("Invalid or expired token"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> systemAdminService.requireAdmin(INVALID_TOKEN));

        assertEquals("Invalid or expired token", ex.getMessage());
    }

    @Test
    @DisplayName("Given non-admin token, when requiring admin, then permission exception is thrown")
    void givenNonAdminToken_WhenRequiringAdmin_ThenPermissionExceptionIsThrown() {
        when(userService.getMemberByToken(VALID_TOKEN)).thenReturn(nonAdminMember);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.requireAdmin(VALID_TOKEN));

        assertEquals("Only system admins can perform this action", ex.getMessage());
    }
}