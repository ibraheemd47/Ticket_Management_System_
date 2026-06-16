package com.sdnah.Ticket_Management_System_.RobustnessTests;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.AuthTokenService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.InitialStateLoader;

/**
 * Verifies that the system boots correctly under valid configuration and that
 * critical initialization failures are detected and surfaced correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("System Initialization — Robustness Tests")
class SystemInitializationTest {

    @Autowired private AuthTokenService authTokenService;
    @Autowired private UserService userService;
    @Autowired private company_managment_serivce companyService;
    @Autowired private EventService eventService;
    @Autowired private PolicyService policyService;
    @Autowired private ObjectMapper objectMapper;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INIT: application context loads successfully with test profile")
    void contextLoads_withTestProfile() {
        // If we reach this line the context loaded — all critical beans are wired
        assertThat(authTokenService).isNotNull();
        assertThat(userService).isNotNull();
        assertThat(companyService).isNotNull();
        assertThat(eventService).isNotNull();
    }

    @Test
    @DisplayName("INIT: JWT secret produces a valid token on startup")
    void jwtSecret_validOnStartup_tokenCanBeGenerated() {
        String token = authTokenService.generateToken("init-check-user");
        assertThat(token).isNotBlank();
        assertThat(authTokenService.validateToken(token)).isTrue();
    }

    // ── JWT secret validation ─────────────────────────────────────────────────

    @Test
    @DisplayName("INIT: AuthTokenService rejects a secret shorter than 32 characters")
    void authTokenService_shortSecret_throwsOnConstruction() {
        assertThatThrownBy(() -> new AuthTokenService("tooshort", 3600000L))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("INIT: AuthTokenService rejects a blank secret")
    void authTokenService_blankSecret_throwsOnConstruction() {
        assertThatThrownBy(() -> new AuthTokenService("", 3600000L))
                .isInstanceOf(Exception.class);
    }

    // ── InitialStateLoader validation ─────────────────────────────────────────

    @Test
    @DisplayName("INIT: InitialStateLoader skips execution when disabled")
    void initialStateLoader_disabled_doesNotThrow() throws Exception {
        InitialStateLoader loader = new InitialStateLoader(
                userService, companyService, eventService, policyService, objectMapper);
        // enabled=false by default — run() must return silently
        assertThatCode(() -> loader.run()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("INIT: InitialStateLoader fails when enabled but file does not exist")
    void initialStateLoader_fileMissing_throwsIllegalState() {
        InitialStateLoader loader = new InitialStateLoader(
                userService, companyService, eventService, policyService, objectMapper);
        loader.setEnabled(true);
        loader.setInitialStateFile(new org.springframework.core.io.ClassPathResource("nonexistent-file.json"));

        assertThatThrownBy(() -> loader.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("INIT: InitialStateLoader fails when JSON is not an array")
    void initialStateLoader_invalidJson_notArray_throwsIllegalState() {
        InitialStateLoader loader = new InitialStateLoader(
                userService, companyService, eventService, policyService, objectMapper);
        loader.setEnabled(true);
        // JSON object instead of array
        byte[] json = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        loader.setInitialStateFile(new ByteArrayResource(json));

        assertThatThrownBy(() -> loader.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JSON array");
    }

    @Test
    @DisplayName("INIT: InitialStateLoader fails when JSON is malformed")
    void initialStateLoader_malformedJson_throwsIllegalState() {
        InitialStateLoader loader = new InitialStateLoader(
                userService, companyService, eventService, policyService, objectMapper);
        loader.setEnabled(true);
        byte[] json = "NOT_VALID_JSON%%%".getBytes(StandardCharsets.UTF_8);
        loader.setInitialStateFile(new ByteArrayResource(json));

        assertThatThrownBy(() -> loader.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not read");
    }
}