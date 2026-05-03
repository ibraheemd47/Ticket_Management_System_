package com.sdnah.Ticket_Management_System_.endpoints;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sdnah.Ticket_Management_System_.Application_Layer.SystemAdminService;

@RestController
@RequestMapping("/api/system-admin")
public class SystemAdminPage {

    private static final Logger logger = LoggerFactory.getLogger(SystemAdminPage.class);

    private final SystemAdminService systemAdminService;

    public SystemAdminPage(SystemAdminService systemAdminService) {
        this.systemAdminService = systemAdminService;
    }

    @PostMapping("/admins/{targetMemberId}")
    public ResponseEntity<Map<String, Object>> assignSystemAdmin(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String targetMemberId) {

        try {
            String token = extractToken(authorizationHeader);
            systemAdminService.assign_system_admin(token, targetMemberId);

            return buildResponse(
                    HttpStatus.OK,
                    "System admin assigned successfully",
                    Map.of("targetMemberId", targetMemberId));

        } catch (IllegalArgumentException e) {
            logger.warn("assignSystemAdmin failed: {}", e.getMessage());
            return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);

        } catch (Exception e) {
            logger.error("assignSystemAdmin unexpected error", e);
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null);
        }
    }

    @PostMapping("/companies/{companyId}/close")
    public ResponseEntity<Map<String, Object>> closeCompany(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String companyId) {

        try {
            String token = extractToken(authorizationHeader);
            systemAdminService.close_company(token, companyId);

            return buildResponse(
                    HttpStatus.OK,
                    "Company closed successfully",
                    Map.of("companyId", companyId));

        } catch (IllegalArgumentException e) {
            logger.warn("closeCompany failed: {}", e.getMessage());
            return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);

        } catch (UnsupportedOperationException e) {
            logger.warn("closeCompany not implemented: {}", e.getMessage());
            return buildResponse(HttpStatus.NOT_IMPLEMENTED, e.getMessage(), null);

        } catch (Exception e) {
            logger.error("closeCompany unexpected error", e);
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null);
        }
    }

    @PostMapping("/companies/{companyId}/open")
    public ResponseEntity<Map<String, Object>> openCompany(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String companyId) {

        try {
            String token = extractToken(authorizationHeader);
            systemAdminService.open_company(token, companyId);

            return buildResponse(
                    HttpStatus.OK,
                    "Company opened successfully",
                    Map.of("companyId", companyId));

        } catch (IllegalArgumentException e) {
            logger.warn("openCompany failed: {}", e.getMessage());
            return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);

        } catch (UnsupportedOperationException e) {
            logger.warn("openCompany not implemented: {}", e.getMessage());
            return buildResponse(HttpStatus.NOT_IMPLEMENTED, e.getMessage(), null);

        } catch (Exception e) {
            logger.error("openCompany unexpected error", e);
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null);
        }
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }

        String prefix = "Bearer ";
        if (authorizationHeader.startsWith(prefix)) {
            String token = authorizationHeader.substring(prefix.length()).trim();
            if (token.isEmpty()) {
                throw new IllegalArgumentException("Bearer token is missing");
            }
            return token;
        }

        // Allow raw token too, in case you test without Bearer
        return authorizationHeader.trim();
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message,
            Map<String, Object> extra) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("message", message);

        if (extra != null) {
            body.putAll(extra);
        }

        return ResponseEntity.status(status).body(body);
    }
}