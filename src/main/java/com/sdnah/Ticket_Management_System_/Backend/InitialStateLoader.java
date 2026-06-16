package com.sdnah.Ticket_Management_System_.Backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Block;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Row;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Seat;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.SeatedArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.StandingArea;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class InitialStateLoader implements CommandLineRunner {

    private final UserService userService;
    private final company_managment_serivce companyService;
    private final EventService eventService;
    private final PolicyService policyService;
    private final ObjectMapper objectMapper;

    @Value("${app.initial-state.enabled:false}")
    private boolean enabled;

    @Value("${app.initial-state.file:classpath:initial-state.json}")
    private Resource initialStateFile;

    /*
     * Variables saved while the file is running.
     *
     * Example:
     * "saveTokenAs": "u1Token"
     * Later:
     * "token": "$u1Token"
     */
    private final Map<String, String> savedTokens = new HashMap<>();

    /*
     * Example:
     * "saveCompanyAs": "p1"
     * Later:
     * "company": "$p1"
     */
    private final Map<String, UUID> savedCompanies = new HashMap<>();

    /*
     * Example:
     * "saveEventAs": "e1"
     */
    private final Map<String, UUID> savedEvents = new HashMap<>();

    public InitialStateLoader(
            UserService userService,
            company_managment_serivce companyService,
            EventService eventService,
            PolicyService policyService,
            ObjectMapper objectMapper) {

        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.policyService = policyService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (!enabled) {
            System.out.println(
                    "========== Initial state loading is disabled =========="
            );
            return;
        }

        if (!initialStateFile.exists()) {
            throw new IllegalStateException(
                    "Initial state file was not found: " + initialStateFile
            );
        }

        /*
         * Clear variables in case the component is called more than once
         * during tests.
         */
        savedTokens.clear();
        savedCompanies.clear();
        savedEvents.clear();

        System.out.println(
                "========== Loading initial state file =========="
        );

        JsonNode root;

        try {
            root = objectMapper.readTree(
                    initialStateFile.getInputStream()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not read the initial-state file: "
                            + exception.getMessage(),
                    exception
            );
        }

        if (root == null || !root.isArray()) {
            throw new IllegalStateException(
                    "Initial-state file must contain a JSON array."
            );
        }

        int index = 0;

        for (JsonNode actionNode : root) {
            index++;

            try {
                executeAction(actionNode, index);
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Initial state failed at action #"
                                + index
                                + ": "
                                + actionNode
                                + ". Reason: "
                                + exception.getMessage(),
                        exception
                );
            }
        }

        System.out.println(
                "========== Initial state loaded successfully =========="
        );
    }

    private void executeAction(JsonNode node, int index) {

        String action = text(node, "action");

        switch (action) {

            case "register" ->
                    register(node);

            case "verify" ->
                    verify(node);

            case "login" ->
                    login(node);

            case "logout" ->
                    logout(node);

            case "openCompany" ->
                    openCompany(node);

            case "appointOwner" ->
                    appointOwner(node);

            case "confirmOwnerAppointment" ->
                    confirmOwnerAppointment(node);

            case "appointManager" ->
                    appointManager(node);

            case "confirmManagerAppointment" ->
                    confirmManagerAppointment(node);

            case "createEventWithLayout" ->
                    createEventWithLayout(node);

            case "addCompanyCoupon" ->
                    addCompanyCoupon(node);

            default ->
                    throw new IllegalArgumentException(
                            "Unknown initial-state action: " + action
                    );
        }

        System.out.println(
                "Initial state action #"
                        + index
                        + " completed: "
                        + action
        );
    }

    // =========================================================
    // USER ACTIONS
    // =========================================================

    private void register(JsonNode node) {

        String username = text(node, "username");
        String password = text(node, "password");
        String email = text(node, "email");

        String phone = textOrDefault(
                node,
                "phone",
                "0500000000"
        );

        int age = intOrDefault(
                node,
                "age",
                18
        );

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

        String code = textOrDefault(
                node,
                "code",
                "000000"
        );

        userService.verifyAccount(
                username,
                code
        );
    }

    private void login(JsonNode node) {

        String username = text(node, "username");
        String password = text(node, "password");
        String saveTokenAs = text(node, "saveTokenAs");

        String token = userService.login(
                username,
                password
        );

        savedTokens.put(
                saveTokenAs,
                token
        );
    }

    private void logout(JsonNode node) {

        String token = resolveToken(
                text(node, "token")
        );

        userService.logout(token);
    }

    // =========================================================
    // COMPANY ACTIONS
    // =========================================================

    private void openCompany(JsonNode node) {

        String token = resolveToken(
                text(node, "token")
        );

        String companyName = text(
                node,
                "companyName"
        );

        String saveCompanyAs = text(
                node,
                "saveCompanyAs"
        );

        UUID companyId = companyService.openCompany(
                token,
                companyName
        );

        savedCompanies.put(
                saveCompanyAs,
                companyId
        );
    }

    private void appointOwner(JsonNode node) {

        String token = resolveToken(
                text(node, "token")
        );

        UUID companyId = resolveCompany(
                text(node, "company")
        );

        String ownerUsername = text(
                node,
                "ownerUsername"
        );

        Member owner = userService.getMemberByUsername(
                ownerUsername
        );

        /*
         * Current project behavior:
         * appointAdditionalOwner immediately gives the owner role.
         *
         * There is currently no PENDING owner appointment in the
         * company service.
         */
        companyService.appointAdditionalOwner(
                token,
                companyId,
                owner.getMemberId()
        );
    }

    private void confirmOwnerAppointment(JsonNode node) {

        String token = resolveToken(
                text(node, "token")
        );

        UUID companyId = resolveCompany(
                text(node, "company")
        );

        /*
         * Validate that the confirming user is logged in and that
         * the referenced company exists.
         *
         * In the current implementation, the owner role was already
         * added by appointAdditionalOwner.
         */
        Member confirmingMember =
                userService.getMemberByToken(token);

        if (confirmingMember == null) {
            throw new IllegalStateException(
                    "Could not resolve the confirming owner."
            );
        }

        if (companyId == null) {
            throw new IllegalStateException(
                    "Company id is missing."
            );
        }

        System.out.println(
                "Owner appointment confirmed by username="
                        + confirmingMember.getUsername()
                        + ", companyId="
                        + companyId
        );
    }

    private void appointManager(JsonNode node) {

        String token = resolveToken(
                text(node, "token")
        );

        UUID companyId = resolveCompany(
                text(node, "company")
        );

        String managerUsername = text(
                node,
                "managerUsername"
        );

        Member manager = userService.getMemberByUsername(
                managerUsername
        );

        Set<CompanyPermission> permissions =
                parsePermissions(
                        node.get("permissions")
                );

        /*
         * Current project behavior:
         * appointManager immediately gives the manager role.
         */
        companyService.appointManager(
                token,
                companyId,
                manager.getMemberId(),
                permissions
        );
    }

    private void confirmManagerAppointment(JsonNode node) {

        String token = resolveToken(
                text(node, "token")
        );

        UUID companyId = resolveCompany(
                text(node, "company")
        );

        Member confirmingMember =
                userService.getMemberByToken(token);

        if (confirmingMember == null) {
            throw new IllegalStateException(
                    "Could not resolve the confirming manager."
            );
        }

        if (companyId == null) {
            throw new IllegalStateException(
                    "Company id is missing."
            );
        }

        System.out.println(
                "Manager appointment confirmed by username="
                        + confirmingMember.getUsername()
                        + ", companyId="
                        + companyId
        );
    }

    // =========================================================
    // EVENT + LAYOUT
    // =========================================================

    private void createEventWithLayout(JsonNode node) {

        String token = resolveToken(
                text(node, "token")
        );

        UUID companyId = resolveCompany(
                text(node, "company")
        );

        String eventName = text(
                node,
                "eventName"
        );

        String venue = text(
                node,
                "venue"
        );

        String eventTypeText = text(
                node,
                "eventType"
        );

        String saveEventAs = text(
                node,
                "saveEventAs"
        );

        LocalDate startDate = LocalDate.parse(
                text(node, "startDate")
        );

        LocalDate endDate = LocalDate.parse(
                text(node, "endDate")
        );

        EventDto eventDto = new EventDto();

        eventDto.name = eventName;
        eventDto.venue = venue;

        eventDto.eventType = show_type.valueOf(
                eventTypeText.trim().toUpperCase()
        );

        eventDto.startDate =
                startDate.atStartOfDay();

        eventDto.endDate =
                endDate.atStartOfDay();

        EventDto createdEvent =
                companyService.addEvent(
                        token,
                        companyId,
                        eventDto
                );

        if (createdEvent == null || createdEvent.id == null) {
            throw new IllegalStateException(
                    "Event was not created correctly."
            );
        }

        savedEvents.put(
                saveEventAs,
                createdEvent.id
        );

        JsonNode showNode = node.get("show");

        if (showNode == null || !showNode.isObject()) {
            throw new IllegalArgumentException(
                    "Missing show configuration for event: "
                            + eventName
            );
        }

        LocalDate showLocalDate = LocalDate.parse(
                text(showNode, "showDate")
        );

        Date showDate = Date.from(
                showLocalDate
                        .atStartOfDay(
                                ZoneId.systemDefault()
                        )
                        .toInstant()
        );

        String showName = text(
                showNode,
                "name"
        );

        String description = textOrDefault(
                showNode,
                "description",
                "Initial-state show"
        );

        String singer = textOrDefault(
                showNode,
                "singer",
                "N/A"
        );

        show newShow = new show(
                createdEvent.id,
                showName,
                description,
                singer,
                showDate
        );

        BigDecimal standingPrice =
                decimalValue(
                        showNode,
                        "standingPrice"
                );

        BigDecimal seatedPrice =
                decimalValue(
                        showNode,
                        "seatedPrice"
                );

        newShow.setStandingPrice(
                standingPrice
        );

        newShow.setSeatedPrice(
                seatedPrice
        );

        List<Area> areas = new ArrayList<>();

        int standingCapacity =
                intOrDefault(
                        showNode,
                        "standingCapacity",
                        0
                );

        if (standingCapacity > 0) {
            StandingArea standingArea =
                    new StandingArea(
                            "Standing Area",
                            standingCapacity
                    );

            areas.add(standingArea);
        }

        int numberOfBlocks =
                intOrDefault(
                        showNode,
                        "numberOfBlocks",
                        0
                );

        int rowsPerBlock =
                intOrDefault(
                        showNode,
                        "rowsPerBlock",
                        0
                );

        int seatsPerRow =
                intOrDefault(
                        showNode,
                        "seatsPerRow",
                        0
                );

        if (numberOfBlocks > 0
                && rowsPerBlock > 0
                && seatsPerRow > 0) {

            SeatedArea seatedArea =
                    createSeatedArea(
                            "Seated Area",
                            numberOfBlocks,
                            rowsPerBlock,
                            seatsPerRow
                    );

            areas.add(seatedArea);
        }

        newShow.setAreas(areas);

        Member actor =
                userService.getMemberByToken(token);

        eventService.addShowToEvent(
                createdEvent.id,
                newShow,
                actor.getMemberId()
        );
    }

    private SeatedArea createSeatedArea(
            String name,
            int numberOfBlocks,
            int rowsPerBlock,
            int seatsPerRow) {

        SeatedArea seatedArea =
                new SeatedArea(
                        name,
                        numberOfBlocks
                );

        List<Block> blocks = new ArrayList<>();

        long idSequence = 1;

        for (
                int blockIndex = 0;
                blockIndex < numberOfBlocks;
                blockIndex++
        ) {

            String blockName =
                    String.valueOf(
                            (char) ('A' + blockIndex)
                    );

            Block block = new Block(
                    idSequence++,
                    blockName,
                    rowsPerBlock,
                    seatedArea
            );

            List<Row> rows = new ArrayList<>();

            for (
                    int rowIndex = 0;
                    rowIndex < rowsPerBlock;
                    rowIndex++
            ) {

                Row row = new Row(
                        idSequence++,
                        String.valueOf(rowIndex + 1),
                        seatsPerRow,
                        block
                );

                List<Seat> seats =
                        new ArrayList<>();

                for (
                        int seatIndex = 1;
                        seatIndex <= seatsPerRow;
                        seatIndex++
                ) {

                    Seat seat = new Seat(
                            idSequence++,
                            String.valueOf(seatIndex),
                            row
                    );

                    seats.add(seat);
                }

                row.setSeats(seats);
                rows.add(row);
            }

            block.setRows(rows);
            blocks.add(block);
        }

        seatedArea.setBlocks(blocks);

        return seatedArea;
    }

    // =========================================================
    // POLICY ACTIONS
    // =========================================================

    private void addCompanyCoupon(JsonNode node) {

        String token = resolveToken(
                text(node, "token")
        );

        UUID companyId = resolveCompany(
                text(node, "company")
        );

        String couponCode = text(
                node,
                "couponCode"
        );

        double percentage = doubleValue(
                node,
                "percentage"
        );

        CouponDiscountRule coupon =
                new CouponDiscountRule(
                        percentage,
                        couponCode
                );

        policyService.addDiscountRuleToCompany(
                token,
                companyId,
                coupon
        );
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================

    private Set<CompanyPermission> parsePermissions(
            JsonNode permissionsNode
    ) {

        Set<CompanyPermission> result =
                new HashSet<>();

        if (permissionsNode == null
                || !permissionsNode.isArray()) {

            result.add(
                    CompanyPermission.MANAGE_EVENTS
            );

            return result;
        }

        Iterator<JsonNode> iterator =
                permissionsNode.elements();

        while (iterator.hasNext()) {

            String permissionName =
                    iterator.next()
                            .asText()
                            .trim()
                            .toUpperCase();

            /*
             * The current CompanyPermission enum does not contain
             * EDIT_VENUE_LAYOUT.
             *
             * For the current project, MANAGE_EVENTS is the permission
             * used for event and layout management.
             */
            if (
                    permissionName.equals(
                            "EDIT_VENUE_LAYOUT"
                    )
            ) {
                result.add(
                        CompanyPermission.MANAGE_EVENTS
                );
                continue;
            }

            /*
             * Managers currently cannot modify policies because
             * PolicyAuthorizationDomainService allows company owners.
             *
             * Therefore we do not grant anything for MANAGE_POLICIES.
             */
            if (
                    permissionName.equals(
                            "MANAGE_POLICIES"
                    )
            ) {
                continue;
            }

            try {
                result.add(
                        CompanyPermission.valueOf(
                                permissionName
                        )
                );
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unknown company permission: "
                                + permissionName,
                        exception
                );
            }
        }

        return result;
    }

    // =========================================================
    // VARIABLE RESOLUTION
    // =========================================================

    private String resolveToken(String value) {

        if (value.startsWith("$")) {

            String key = value.substring(1);

            String token =
                    savedTokens.get(key);

            if (token == null) {
                throw new IllegalArgumentException(
                        "Token variable was not found: "
                                + value
                );
            }

            return token;
        }

        return value;
    }

    private UUID resolveCompany(String value) {

        if (value.startsWith("$")) {

            String key = value.substring(1);

            UUID companyId =
                    savedCompanies.get(key);

            if (companyId == null) {
                throw new IllegalArgumentException(
                        "Company variable was not found: "
                                + value
                );
            }

            return companyId;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid company UUID: " + value,
                    exception
            );
        }
    }

    @SuppressWarnings("unused")
    private UUID resolveEvent(String value) {

        if (value.startsWith("$")) {

            String key = value.substring(1);

            UUID eventId =
                    savedEvents.get(key);

            if (eventId == null) {
                throw new IllegalArgumentException(
                        "Event variable was not found: "
                                + value
                );
            }

            return eventId;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid event UUID: " + value,
                    exception
            );
        }
    }

    // =========================================================
    // JSON HELPERS
    // =========================================================

    private String text(
            JsonNode node,
            String fieldName
    ) {

        JsonNode value = node.get(fieldName);

        if (
                value == null
                        || value.isNull()
                        || value.asText().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Missing required field: "
                            + fieldName
            );
        }

        return value.asText().trim();
    }

    private String textOrDefault(
            JsonNode node,
            String fieldName,
            String defaultValue
    ) {

        JsonNode value = node.get(fieldName);

        if (
                value == null
                        || value.isNull()
                        || value.asText().isBlank()
        ) {
            return defaultValue;
        }

        return value.asText().trim();
    }

    private int intOrDefault(
            JsonNode node,
            String fieldName,
            int defaultValue
    ) {

        JsonNode value = node.get(fieldName);

        if (
                value == null
                        || value.isNull()
        ) {
            return defaultValue;
        }

        if (!value.canConvertToInt()) {
            throw new IllegalArgumentException(
                    "Field must be an integer: "
                            + fieldName
            );
        }

        return value.asInt();
    }

    private double doubleValue(
            JsonNode node,
            String fieldName
    ) {

        JsonNode value = node.get(fieldName);

        if (
                value == null
                        || value.isNull()
                        || !value.isNumber()
        ) {
            throw new IllegalArgumentException(
                    "Missing or invalid numeric field: "
                            + fieldName
            );
        }

        return value.asDouble();
    }

    private BigDecimal decimalValue(
            JsonNode node,
            String fieldName
    ) {

        JsonNode value = node.get(fieldName);

        if (
                value == null
                        || value.isNull()
                        || !value.isNumber()
        ) {
            throw new IllegalArgumentException(
                    "Missing or invalid decimal field: "
                            + fieldName
            );
        }

        return value.decimalValue();
    }
}