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

    @Test
    void login_withValidCredentials_returnsTokenAndMarksUserLoggedIn() {
        AuthToken token = new AuthToken("token-123", "m1", LocalDateTime.now().plusHours(2));

        member.logout();

        when(userRepository.findByUsername("mostafa")).thenReturn(Optional.of(member));
        when(passwordHasher.matches("123456", "hashed-password")).thenReturn(true);
        when(authTokenService.issueToken("m1")).thenReturn(token);

        String result = userService.login("mostafa", "123456");

        assertEquals("token-123", result);
        assertTrue(member.isLoggedin());
        verify(userRepository).save(member);
        verify(tokenRepository).save(token);
    }

    @Test
    void login_withUnknownUsername_throwsException() {
        when(userRepository.findByUsername("mostafa")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("mostafa", "123456"));

        assertEquals("Invalid username or password", ex.getMessage());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void login_withWrongPassword_throwsException() {
        when(userRepository.findByUsername("mostafa")).thenReturn(Optional.of(member));
        when(passwordHasher.matches("wrong-pass", "hashed-password")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("mostafa", "wrong-pass"));

        assertEquals("Invalid username or password", ex.getMessage());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void login_withInactiveMember_throwsException() {
        Member inactiveMember = mock(Member.class);
        when(inactiveMember.isActive()).thenReturn(false);

        when(userRepository.findByUsername("mostafa")).thenReturn(Optional.of(inactiveMember));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("mostafa", "123456"));

        assertEquals("Member is inactive", ex.getMessage());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void logout_withValidToken_logsOutMemberAndDeletesToken() {
        AuthToken token = new AuthToken("token-123", "m1", LocalDateTime.now().plusHours(2));

        when(tokenRepository.findById("token-123")).thenReturn(Optional.of(token));
        when(userRepository.findById("m1")).thenReturn(Optional.of(member));

        userService.logout("token-123");

        assertFalse(member.isLoggedin());
        verify(userRepository).save(member);
        verify(tokenRepository).deleteBytokenValue("token-123");
    }

    @Test
    void logout_withBlankToken_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.logout("   "));

        assertEquals("Token cannot be empty", ex.getMessage());
    }

    @Test
    void getMemberByToken_withValidToken_returnsMember() {
        AuthToken token = new AuthToken("token-123", "m1", LocalDateTime.now().plusHours(2));

        when(tokenRepository.findById("token-123")).thenReturn(Optional.of(token));
        when(userRepository.findById("m1")).thenReturn(Optional.of(member));

        Member result = userService.getMemberByToken("token-123");

        assertEquals(member, result);
    }

    @Test
    void getMemberByToken_withExpiredToken_deletesTokenAndThrowsException() {
        AuthToken expired = new AuthToken("token-123", "m1", LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findById("token-123")).thenReturn(Optional.of(expired));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getMemberByToken("token-123"));

        assertEquals("Token expired", ex.getMessage());
        verify(tokenRepository).deleteBytokenValue("token-123");
    }

    @Test
    void isTokenValid_returnsTrueForExistingFreshToken() {
        AuthToken token = new AuthToken("token-123", "m1", LocalDateTime.now().plusHours(2));
        when(tokenRepository.findById("token-123")).thenReturn(Optional.of(token));

        assertTrue(userService.isTokenValid("token-123"));
    }

    @Test
    void isTokenValid_returnsFalseForBlankToken() {
        assertFalse(userService.isTokenValid("   "));
        verify(tokenRepository, never()).findById(any());
    }

    @Test
    void isTokenValid_returnsFalseForMissingToken() {
        when(tokenRepository.findById("token-123")).thenReturn(Optional.empty());

        assertFalse(userService.isTokenValid("token-123"));
    }

    @Test
    void isTokenValid_returnsFalseAndDeletesWhenExpired() {
        AuthToken expired = new AuthToken("token-123", "m1", LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findById("token-123")).thenReturn(Optional.of(expired));

        assertFalse(userService.isTokenValid("token-123"));
        verify(tokenRepository).deleteBytokenValue("token-123");
    }

}
