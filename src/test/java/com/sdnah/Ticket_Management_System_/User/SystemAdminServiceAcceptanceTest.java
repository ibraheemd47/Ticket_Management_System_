package com.sdnah.Ticket_Management_System_.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Application_Layer.SystemAdminService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.System_admin;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.SystemAdminRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

@ExtendWith(MockitoExtension.class)
class SystemAdminServiceAcceptanceTest {

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
        validToken = new AuthToken("token-123", "admin-1", LocalDateTime.now().plusHours(2));
        targetMember = new Member("member-1", "mostafa", "hash");
    }

    @Test
    void assignSystemAdmin_byExistingAdmin_savesNewAdmin() {
        when(tokenRepository.findByTokenValue("token-123")).thenReturn(validToken);
        when(systemAdminRepository.existsById("admin-1")).thenReturn(true);
        when(userRepository.findById("member-1")).thenReturn(Optional.of(targetMember));
        when(systemAdminRepository.existsById("member-1")).thenReturn(false);

        systemAdminService.assign_system_admin("token-123", "member-1");

        verify(systemAdminRepository).save(any(System_admin.class));
        verify(userRepository).save(any(Member.class));
    }

    @Test
    void assignSystemAdmin_withInvalidToken_throwsException() {
        when(tokenRepository.findByTokenValue("bad-token")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin("bad-token", "member-1"));

        assertEquals("Invalid token for admin", ex.getMessage());
    }

    @Test
    void assignSystemAdmin_byNonAdmin_throwsException() {
        when(tokenRepository.findByTokenValue("token-123")).thenReturn(validToken);
        when(systemAdminRepository.existsById("admin-1")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin("token-123", "member-1"));

        assertEquals("Only system admins can assign new admins", ex.getMessage());
    }

    @Test
    void assignSystemAdmin_whenTargetNotFound_throwsException() {
        when(tokenRepository.findByTokenValue("token-123")).thenReturn(validToken);
        when(systemAdminRepository.existsById("admin-1")).thenReturn(true);
        when(userRepository.findById("member-1")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin("token-123", "member-1"));

        assertEquals("Member not found", ex.getMessage());
    }

    @Test
    void assignSystemAdmin_whenTargetAlreadyAdmin_throwsException() {
        when(tokenRepository.findByTokenValue("token-123")).thenReturn(validToken);
        when(systemAdminRepository.existsById("admin-1")).thenReturn(true);
        when(userRepository.findById("member-1")).thenReturn(Optional.of(targetMember));
        when(systemAdminRepository.existsById("member-1")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> systemAdminService.assign_system_admin("token-123", "member-1"));

        assertEquals("Member is already an admin", ex.getMessage());
    }
}
