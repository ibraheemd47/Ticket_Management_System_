package com.sdnah.Ticket_Management_System_.User.IntegrationTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sdnah.Ticket_Management_System_.Application_Layer.PasswordHasher;
import com.sdnah.Ticket_Management_System_.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.AuthToken;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.Member;
import com.sdnah.Ticket_Management_System_.Domain_Layer.User.VerificationEmail;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TokenRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.UserRepository;
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
    private TokenRepository tokenRepository;

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
    @DisplayName("Given registered user, when login, then token is created and saved")
    void givenRegisteredUser_WhenLogin_ThenTokenCreatedAndSaved() {
        // Arrange
        userService.register(
                "mostafa",
                "123456",
                "mostafa@test.com",
                "0501234567",
                VerificationMethod.EMAIL);

        // Act
        String token = userService.login("mostafa", "123456");

        // Assert
        assertTrue(tokenRepository.existsByTokenValue(token));
    }

    @Test
    @DisplayName("Given valid token, when logout, then token is removed")
    void givenValidToken_WhenLogout_ThenTokenRemoved() {
        // Arrange
        userService.register(
                "mostafa",
                "123456",
                "mostafa@test.com",
                "0501234567",
                VerificationMethod.EMAIL);

        String token = userService.login("mostafa", "123456");

        // Act
        userService.logout(token);

        // Assert
        assertFalse(tokenRepository.existsByTokenValue(token));
    }

    @Test
    @DisplayName("Given valid token, when getMemberByToken, then correct member returned")
    void givenValidToken_WhenGetMemberByToken_ThenCorrectMemberReturned() {
        // Arrange
        String id = userService.register(
                "mostafa",
                "123456",
                "mostafa@test.com",
                "0501234567",
                VerificationMethod.EMAIL);

        String token = userService.login("mostafa", "123456");

        // Act
        Member member = userService.getMemberByToken(token);

        // Assert
        assertEquals(id, member.getMemberId());
    }
}
