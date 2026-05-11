package com.sdnah.Ticket_Management_System_.User.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.UserRepository;
import com.sdnah.Ticket_Management_System_.User.IntegrationTests.testconfig.TestConfig;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Test
    @DisplayName("Given valid details, when user registers, then member is saved in DB")
    void givenValidDetails_WhenUserRegisters_ThenMemberIsSavedInDB() {
        // Act
        String memberId = userService.register(
                "mostafa",
                "123456",
                "mostafa@test.com",
                "0501234567",
                VerificationMethod.EMAIL);

        // Assert
        Member saved = userRepository.findById(memberId).orElseThrow();

        assertEquals("mostafa", saved.getUsername());
        assertEquals("hashed-123456", saved.getPasswordHash());
        assertFalse(saved.isVerified());
        assertEquals("123456", saved.getVerificationCode());
    }

    @Test
    @DisplayName("Given verified registered user, when login, then JWT token is created")
    void givenVerifiedRegisteredUser_WhenLogin_ThenJwtTokenIsCreated() {
        // Arrange
        String memberId = userService.register(
                "mostafa",
                "123456",
                "mostafa@test.com",
                "0501234567",
                VerificationMethod.EMAIL);

        userService.verifyAccount("mostafa", "123456");

        // Act
        String token = userService.login("mostafa", "123456");

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(authTokenService.validateToken(token));
        assertEquals("mostafa", authTokenService.extractUsername(token));

        Member saved = userRepository.findById(memberId).orElseThrow();
        assertTrue(saved.isLoggedin());
        // JWTs are stateless — there is no DB-backed token table to assert against.
    }

    @Test
    @DisplayName("Given valid JWT token, when logout, then member is logged out")
    void givenValidJwtToken_WhenLogout_ThenMemberIsLoggedOut() {
        // Arrange
        String memberId = userService.register(
                "mostafa",
                "123456",
                "mostafa@test.com",
                "0501234567",
                VerificationMethod.EMAIL);

        userService.verifyAccount("mostafa", "123456");

        String token = userService.login("mostafa", "123456");

        // Act
        userService.logout(token);

        // Assert
        Member saved = userRepository.findById(memberId).orElseThrow();
        assertFalse(saved.isLoggedin());
        // JWT is stateless — no DB row to delete on logout.
    }

    @Test
    @DisplayName("Given valid JWT token, when getMemberByToken, then correct member returned")
    void givenValidJwtToken_WhenGetMemberByToken_ThenCorrectMemberReturned() {
        // Arrange
        String id = userService.register(
                "mostafa",
                "123456",
                "mostafa@test.com",
                "0501234567",
                VerificationMethod.EMAIL);

        userService.verifyAccount("mostafa", "123456");

        String token = userService.login("mostafa", "123456");

        // Act
        Member member = userService.getMemberByToken(token);

        // Assert
        assertEquals(id, member.getMemberId());
        assertEquals("mostafa", member.getUsername());
    }

    @Test
    @DisplayName("Given unverified registered user, when login, then account not verified exception is thrown")
    void givenUnverifiedRegisteredUser_WhenLogin_ThenAccountNotVerifiedExceptionIsThrown() {
        // Arrange
        userService.register(
                "mostafa",
                "123456",
                "mostafa@test.com",
                "0501234567",
                VerificationMethod.EMAIL);

        // Act
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("mostafa", "123456"));

        // Assert
        assertEquals("Account is not verified", ex.getMessage());
    }
}