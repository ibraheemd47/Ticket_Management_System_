package com.sdnah.Ticket_Management_System_.Backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.PolicyService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.VerificationMethod;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.CouponDiscountRule;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;

/**
 * Tests for {@link InitialStateLoader}. Verifies the JSON-driven action
 * runner — file/parse errors, every action type, variable resolution, and
 * permission parsing — using mocks for the four downstream services.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InitialStateLoader — JSON-driven startup actions")
class InitialStateLoaderTest {

    @Mock private UserService userService;
    @Mock private company_managment_serivce companyService;
    @Mock private EventService eventService;
    @Mock private PolicyService policyService;

    private InitialStateLoader loader;

    @BeforeEach
    void setUp() {
        loader = new InitialStateLoader(
                userService, companyService, eventService, policyService,
                new ObjectMapper());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static Resource json(String body) {
        return new ByteArrayResource(body.getBytes(StandardCharsets.UTF_8));
    }

    private void runWith(String jsonBody) throws Exception {
        loader.setEnabled(true);
        loader.setInitialStateFile(json(jsonBody));
        loader.run();
    }

    // ── enabled flag ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Disabled by default — nothing is read, no service called")
    void disabled_doesNothing() throws Exception {
        // Don't enable; don't even set a file
        loader.run();

        verifyNoInteractions(userService, companyService, eventService, policyService);
    }

    // ── file/JSON errors ────────────────────────────────────────────────────

    @Test
    @DisplayName("Enabled but file missing → throws IllegalStateException")
    void enabledButFileMissing_throws() {
        loader.setEnabled(true);
        loader.setInitialStateFile(new ByteArrayResource(new byte[0]) {
            @Override public boolean exists() { return false; }
        });

        assertThatThrownBy(() -> loader.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("File is not a JSON array → throws IllegalStateException")
    void notAnArray_throws() {
        assertThatThrownBy(() -> runWith("{\"action\":\"register\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must contain a JSON array");
    }

    @Test
    @DisplayName("Malformed JSON → throws IllegalStateException with parse hint")
    void malformedJson_throws() {
        assertThatThrownBy(() -> runWith("not-json-at-all"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not read");
    }

    @Test
    @DisplayName("Empty array → succeeds, calls nothing")
    void emptyArray_noOp() throws Exception {
        runWith("[]");
        verifyNoInteractions(userService, companyService, eventService, policyService);
    }

    @Test
    @DisplayName("Unknown action → wrapped error includes action index + body")
    void unknownAction_wrapsWithIndex() {
        assertThatThrownBy(() -> runWith("[{\"action\":\"doSomethingWeird\"}]"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Initial state failed at action #1")
                .hasMessageContaining("Unknown initial-state action: doSomethingWeird");
    }

    // ── register / verify / login / logout ──────────────────────────────────

    @Test
    @DisplayName("register passes username/password/email/phone/age + EMAIL method")
    void register_callsUserService() throws Exception {
        runWith("""
                [{
                  "action":"register",
                  "username":"alice",
                  "password":"pass1234",
                  "email":"a@x.com",
                  "phone":"0501234567",
                  "age":25
                }]
                """);

        verify(userService).register(
                eq("alice"), eq("pass1234"), eq("a@x.com"),
                eq("0501234567"), eq(25), eq(VerificationMethod.EMAIL));
    }

    @Test
    @DisplayName("register applies default phone + age when fields are missing")
    void register_appliesDefaults() throws Exception {
        runWith("""
                [{
                  "action":"register",
                  "username":"alice",
                  "password":"pass1234",
                  "email":"a@x.com"
                }]
                """);

        verify(userService).register(
                eq("alice"), eq("pass1234"), eq("a@x.com"),
                eq("0500000000"), eq(18), eq(VerificationMethod.EMAIL));
    }

    @Test
    @DisplayName("verify with explicit code calls UserService.verifyAccount")
    void verify_callsVerifyAccount() throws Exception {
        runWith("""
                [{"action":"verify","username":"alice","code":"123456"}]
                """);
        verify(userService).verifyAccount("alice", "123456");
    }

    @Test
    @DisplayName("login stores the returned token under saveTokenAs; logout resolves it")
    void loginThenLogout_resolvesTokenVariable() throws Exception {
        when(userService.login("alice", "pw")).thenReturn("jwt-for-alice");

        runWith("""
                [
                  {"action":"login","username":"alice","password":"pw","saveTokenAs":"alice"},
                  {"action":"logout","token":"$alice"}
                ]
                """);

        verify(userService).login("alice", "pw");
        verify(userService).logout("jwt-for-alice");
    }

    @Test
    @DisplayName("Unresolved $token variable → action fails fast")
    void unresolvedTokenVariable_fails() {
        assertThatThrownBy(() -> runWith(
                "[{\"action\":\"logout\",\"token\":\"$ghost\"}]"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Token variable was not found");
    }

    // ── openCompany + appointOwner / appointManager ─────────────────────────

    @Test
    @DisplayName("openCompany stores returned UUID, appointOwner resolves it")
    void openCompanyThenAppoint_resolvesCompanyVariable() throws Exception {
        when(userService.login(anyString(), anyString())).thenReturn("jwt-alice");
        UUID companyId = UUID.randomUUID();
        when(companyService.openCompany("jwt-alice", "Acme")).thenReturn(companyId);

        Member bob = new Member("m-bob", "bob", "h");
        when(userService.getMemberByUsername("bob")).thenReturn(bob);

        runWith("""
                [
                  {"action":"login","username":"alice","password":"pw","saveTokenAs":"alice"},
                  {"action":"openCompany","token":"$alice","companyName":"Acme","saveCompanyAs":"acme"},
                  {"action":"appointOwner","token":"$alice","company":"$acme","ownerUsername":"bob"}
                ]
                """);

        verify(companyService).appointAdditionalOwner("jwt-alice", companyId, "m-bob");
    }

    @Test
    @DisplayName("Unresolved $company variable → action fails fast")
    void unresolvedCompanyVariable_fails() {
        assertThatThrownBy(() -> runWith("""
                [{"action":"appointOwner",
                  "token":"raw-token",
                  "company":"$ghostCompany",
                  "ownerUsername":"bob"}]
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Company variable was not found");
    }

    @Test
    @DisplayName("Company as raw UUID parses, but a non-UUID string is rejected")
    void companyRawUuid_andInvalidUuid() throws Exception {
        UUID raw = UUID.randomUUID();
        when(userService.getMemberByUsername("bob"))
                .thenReturn(new Member("m-bob", "bob", "h"));

        // raw uuid works
        runWith("""
                [{"action":"appointOwner",
                  "token":"raw-token",
                  "company":"%s",
                  "ownerUsername":"bob"}]
                """.formatted(raw));
        verify(companyService).appointAdditionalOwner("raw-token", raw, "m-bob");

        // invalid uuid is rejected
        assertThatThrownBy(() -> runWith("""
                [{"action":"appointOwner",
                  "token":"raw-token",
                  "company":"not-a-uuid",
                  "ownerUsername":"bob"}]
                """))
                .hasMessageContaining("Invalid company UUID");
    }

    // ── appointManager + permission parsing ─────────────────────────────────

    @Test
    @DisplayName("appointManager with no permissions array → defaults to MANAGE_EVENTS")
    void appointManager_defaultPermissions() throws Exception {
        when(userService.getMemberByUsername("manager-bob"))
                .thenReturn(new Member("m-bob", "manager-bob", "h"));
        UUID companyId = UUID.randomUUID();

        runWith("""
                [{"action":"appointManager",
                  "token":"raw-token",
                  "company":"%s",
                  "managerUsername":"manager-bob"}]
                """.formatted(companyId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<CompanyPermission>> captor = ArgumentCaptor.forClass(Set.class);
        verify(companyService).appointManager(
                eq("raw-token"), eq(companyId), eq("m-bob"), captor.capture());
        assertThat(captor.getValue()).containsExactly(CompanyPermission.MANAGE_EVENTS);
    }

    @Test
    @DisplayName("EDIT_VENUE_LAYOUT permission maps to MANAGE_EVENTS; MANAGE_POLICIES is dropped")
    void appointManager_permissionAliases() throws Exception {
        when(userService.getMemberByUsername("manager-bob"))
                .thenReturn(new Member("m-bob", "manager-bob", "h"));
        UUID companyId = UUID.randomUUID();

        runWith("""
                [{"action":"appointManager",
                  "token":"raw-token",
                  "company":"%s",
                  "managerUsername":"manager-bob",
                  "permissions":["EDIT_VENUE_LAYOUT","MANAGE_POLICIES"]}]
                """.formatted(companyId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<CompanyPermission>> captor = ArgumentCaptor.forClass(Set.class);
        verify(companyService).appointManager(
                eq("raw-token"), eq(companyId), eq("m-bob"), captor.capture());
        // EDIT_VENUE_LAYOUT → MANAGE_EVENTS, MANAGE_POLICIES → skipped
        assertThat(captor.getValue()).containsExactly(CompanyPermission.MANAGE_EVENTS);
    }

    @Test
    @DisplayName("Unknown permission name → throws with the offending value in the message")
    void appointManager_unknownPermission_throws() {
        when(userService.getMemberByUsername("manager-bob"))
                .thenReturn(new Member("m-bob", "manager-bob", "h"));
        UUID companyId = UUID.randomUUID();

        assertThatThrownBy(() -> runWith("""
                [{"action":"appointManager",
                  "token":"raw-token",
                  "company":"%s",
                  "managerUsername":"manager-bob",
                  "permissions":["NOT_A_REAL_PERMISSION"]}]
                """.formatted(companyId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown company permission: NOT_A_REAL_PERMISSION");
    }

    // ── confirm actions ─────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmOwnerAppointment just validates state; no service write")
    void confirmOwnerAppointment_isNoMutation() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(userService.getMemberByToken("raw-token"))
                .thenReturn(new Member("m-bob", "bob", "h"));

        runWith("""
                [{"action":"confirmOwnerAppointment",
                  "token":"raw-token",
                  "company":"%s"}]
                """.formatted(companyId));

        verify(userService).getMemberByToken("raw-token");
        verify(companyService, never()).appointAdditionalOwner(any(), any(), any());
    }

    // ── addCompanyCoupon ────────────────────────────────────────────────────

    @Test
    @DisplayName("addCompanyCoupon builds a CouponDiscountRule and delegates to PolicyService")
    void addCompanyCoupon_callsPolicyService() throws Exception {
        UUID companyId = UUID.randomUUID();

        runWith("""
                [{"action":"addCompanyCoupon",
                  "token":"raw-token",
                  "company":"%s",
                  "couponCode":"SAVE10",
                  "percentage":10.0}]
                """.formatted(companyId));

        ArgumentCaptor<CouponDiscountRule> ruleCap = ArgumentCaptor.forClass(CouponDiscountRule.class);
        verify(policyService).addDiscountRuleToCompany(
                eq("raw-token"), eq(companyId), ruleCap.capture());
        CouponDiscountRule rule = ruleCap.getValue();
        assertThat(rule).isNotNull();
    }

    // ── createEventWithLayout ───────────────────────────────────────────────

    @Test
    @DisplayName("createEventWithLayout adds the event then adds the show")
    void createEventWithLayout_addsEventAndShow() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID createdEventId = UUID.randomUUID();
        EventDto returned = new EventDto();
        returned.id = createdEventId;
        when(companyService.addEvent(eq("raw-token"), eq(companyId), any()))
                .thenReturn(returned);
        when(userService.getMemberByToken("raw-token"))
                .thenReturn(new Member("m-actor", "actor", "h"));

        runWith("""
                [{"action":"createEventWithLayout",
                  "token":"raw-token",
                  "company":"%s",
                  "eventName":"Opening Night",
                  "venue":"Main Hall",
                  "eventType":"PERFORMANCE",
                  "saveEventAs":"e1",
                  "startDate":"2026-09-01",
                  "endDate":"2026-09-02",
                  "show":{
                    "name":"Show A",
                    "showDate":"2026-09-01",
                    "standingPrice":50.0,
                    "seatedPrice":80.0,
                    "standingCapacity":100,
                    "numberOfBlocks":2,
                    "rowsPerBlock":3,
                    "seatsPerRow":4
                  }}]
                """.formatted(companyId));

        verify(companyService).addEvent(eq("raw-token"), eq(companyId), any());
        verify(eventService).addShowToEvent(eq(createdEventId), any(), eq("m-actor"));
    }

    @Test
    @DisplayName("createEventWithLayout fails fast when addEvent returns no id")
    void createEventWithLayout_failsIfEventNotCreated() {
        UUID companyId = UUID.randomUUID();
        EventDto noId = new EventDto();   // id remains null
        when(companyService.addEvent(any(), any(), any())).thenReturn(noId);

        assertThatThrownBy(() -> runWith("""
                [{"action":"createEventWithLayout",
                  "token":"raw-token",
                  "company":"%s",
                  "eventName":"Bad Event",
                  "venue":"Main Hall",
                  "eventType":"PERFORMANCE",
                  "saveEventAs":"e1",
                  "startDate":"2026-09-01",
                  "endDate":"2026-09-02",
                  "show":{
                    "name":"Show A","showDate":"2026-09-01",
                    "standingPrice":10.0,"seatedPrice":20.0,
                    "standingCapacity":1,"numberOfBlocks":0,
                    "rowsPerBlock":0,"seatsPerRow":0}}]
                """.formatted(companyId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Event was not created correctly");
    }

    @Test
    @DisplayName("createEventWithLayout requires the show subobject")
    void createEventWithLayout_missingShow_throws() {
        UUID companyId = UUID.randomUUID();
        EventDto returned = new EventDto();
        returned.id = UUID.randomUUID();
        when(companyService.addEvent(any(), any(), any())).thenReturn(returned);

        assertThatThrownBy(() -> runWith("""
                [{"action":"createEventWithLayout",
                  "token":"raw-token",
                  "company":"%s",
                  "eventName":"Bad Event",
                  "venue":"Main Hall",
                  "eventType":"PERFORMANCE",
                  "saveEventAs":"e1",
                  "startDate":"2026-09-01",
                  "endDate":"2026-09-02"}]
                """.formatted(companyId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing show configuration");
    }

    // ── action index wrapping ───────────────────────────────────────────────

    @Test
    @DisplayName("A failure on the 3rd action is wrapped with action #3 in the message")
    void actionIndex_isReportedOnFailure() {
        // Action 1 + 2 succeed (register), then action 3 fails (unknown action)
        assertThatThrownBy(() -> runWith("""
                [
                  {"action":"register","username":"a","password":"p1","email":"a@x.com"},
                  {"action":"register","username":"b","password":"p1","email":"b@x.com"},
                  {"action":"bogusAction"}
                ]
                """))
                .hasMessageContaining("Initial state failed at action #3");
    }

    // ── missing required field ──────────────────────────────────────────────

    @Test
    @DisplayName("Missing required field → throws with the field name")
    void missingRequiredField_throws() {
        // register requires username — omit it
        assertThatThrownBy(() -> runWith(
                "[{\"action\":\"register\",\"password\":\"pw\",\"email\":\"a@x.com\"}]"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing required field: username");
    }
}
