package com.sdnah.Ticket_Management_System_.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.UserRole;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceAcceptanceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private UserService userService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member("m1", "mostafa", "hashed-password");
    }

    @Test
    void register_withValidInput_savesNewMemberAndReturnsTrue() {
        when(userRepository.existsByUsername("mostafa")).thenReturn(false);
        when(passwordHasher.hash("123456")).thenReturn("hashed-123456");
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        boolean result = userService.register("mostafa", "123456");

        assertTrue(result);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(userRepository).save(memberCaptor.capture());

        Member savedMember = memberCaptor.getValue();
        assertEquals("mostafa", savedMember.getUsername());
        assertEquals("hashed-123456", savedMember.getPasswordHash());
        assertEquals(UserRole.MEMBER, savedMember.getRole());
    }

    @Test
    void register_withExistingUsername_throwsException() {
        when(userRepository.existsByUsername("mostafa")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register("mostafa", "123456"));

        assertEquals("Username already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withBlankUsername_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register("   ", "123456"));

        assertEquals("Username cannot be empty", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withShortUsername_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register("ab", "123456"));

        assertEquals("Username must contain at least 3 characters", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withShortPassword_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register("mostafa", "123"));

        assertEquals("Password must contain at least 6 characters", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

}
