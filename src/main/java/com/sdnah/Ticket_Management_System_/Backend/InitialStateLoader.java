package com.sdnah.Ticket_Management_System_.Backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class InitialStateLoader implements CommandLineRunner {

    private final UserService userService;
    private final company_managment_serivce companyService;
    private final ObjectMapper objectMapper;

    @Value("${app.initial-state.enabled:false}")
    private boolean enabled;

    @Value("${app.initial-state.file:classpath:initial-state.json}")
    private Resource initialStateFile;

    private final Map<String, String> savedTokens = new HashMap<>();
    private final Map<String, UUID> savedCompanies = new HashMap<>();

    public InitialStateLoader(UserService userService,
                              company_managment_serivce companyService,
                              ObjectMapper objectMapper) {
        this.userService = userService;
        this.companyService = companyService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!enabled) {
            System.out.println("Initial state loading is disabled.");
            return;
        }

        if (!initialStateFile.exists()) {
            throw new IllegalStateException("Initial state file not found: " + initialStateFile);
        }

        System.out.println("========== Loading initial state file ==========");

        JsonNode root = objectMapper.readTree(initialStateFile.getInputStream());

        if (!root.isArray()) {
            throw new IllegalStateException("Initial state file must be a JSON array.");
        }

        int index = 0;

        for (JsonNode actionNode : root) {
            index++;

            try {
                executeAction(actionNode, index);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Initial state failed at action #" + index + ": " + actionNode + ". Reason: " + e.getMessage(),
                        e
                );
            }
        }

        System.out.println("========== Initial state loaded successfully ==========");
    }

    private void executeAction(JsonNode node, int index) {
        String action = text(node, "action");

        switch (action) {
            case "register" -> register(node);
            case "verify" -> verify(node);
            case "login" -> login(node);
            case "openCompany" -> openCompany(node);
            case "appointManager" -> appointManager(node);
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        }

        System.out.println("Initial state action #" + index + " completed: " + action);
    }

    private void register(JsonNode node) {
        String username = text(node, "username");
        String password = text(node, "password");
        String email = text(node, "email");
        String phone = textOrDefault(node, "phone", "0500000000");
        int age = intOrDefault(node, "age", 18);

        userService.register(
                username,
                password,
                email,
                phone,
                age,
                VerificationMethod.EMAIL
        );
    }

    private void verify(JsonNode node) {
        String username = text(node, "username");
        String code = textOrDefault(node, "code", "000000");

        userService.verifyAccount(username, code);
    }

    private void login(JsonNode node) {
        String username = text(node, "username");
        String password = text(node, "password");
        String saveTokenAs = text(node, "saveTokenAs");

        String token = userService.login(username, password);
        savedTokens.put(saveTokenAs, token);
    }

    private void openCompany(JsonNode node) {
        String token = resolveToken(text(node, "token"));
        String companyName = text(node, "companyName");
        String saveCompanyAs = text(node, "saveCompanyAs");

        UUID companyId = companyService.openCompany(token, companyName);
        savedCompanies.put(saveCompanyAs, companyId);
    }

    private void appointManager(JsonNode node) {
        String token = resolveToken(text(node, "token"));
        UUID companyId = resolveCompany(text(node, "company"));
        String managerUsername = text(node, "managerUsername");

        Member manager = userService.getMemberByUsername(managerUsername);
        String managerId = manager.getMemberId();

        Set<CompanyPermission> permissions = parsePermissions(node.get("permissions"));

        companyService.appointManager(
                token,
                companyId,
                managerId,
                permissions
        );
    }

    private Set<CompanyPermission> parsePermissions(JsonNode permissionsNode) {
        Set<CompanyPermission> result = new HashSet<>();

        if (permissionsNode == null || !permissionsNode.isArray()) {
            result.add(CompanyPermission.MANAGE_EVENTS);
            return result;
        }

        Iterator<JsonNode> iterator = permissionsNode.elements();

        while (iterator.hasNext()) {
            String permissionName = iterator.next().asText();
            result.add(CompanyPermission.valueOf(permissionName));
        }

        return result;
    }

    private String resolveToken(String value) {
        if (value.startsWith("$")) {
            String key = value.substring(1);
            String token = savedTokens.get(key);

            if (token == null) {
                throw new IllegalArgumentException("Token variable not found: " + value);
            }

            return token;
        }

        return value;
    }

    private UUID resolveCompany(String value) {
        if (value.startsWith("$")) {
            String key = value.substring(1);
            UUID companyId = savedCompanies.get(key);

            if (companyId == null) {
                throw new IllegalArgumentException("Company variable not found: " + value);
            }

            return companyId;
        }

        return UUID.fromString(value);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }

        return value.asText();
    }

    private String textOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.asText().isBlank()) {
            return defaultValue;
        }

        return value.asText();
    }

    private int intOrDefault(JsonNode node, String fieldName, int defaultValue) {
        JsonNode value = node.get(fieldName);

        if (value == null || !value.isInt()) {
            return defaultValue;
        }

        return value.asInt();
    }
}