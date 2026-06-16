package com.sdnah.Ticket_Management_System_.wsep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepClient;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepException;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepIssueTicketRequest;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepPaymentRequest;

/**
 * Unit tests for {@link WsepClient}. Uses Spring's {@link MockRestServiceServer}
 * to stand in for the real WSEP endpoint — no network is touched.
 */
@DisplayName("WsepClient — HTTP behaviour")
class WsepClientTest {

    private static final String BASE_URL = "http://wsep.example/";

    private RestTemplate rest;
    private MockRestServiceServer server;
    private WsepClient client;

    @BeforeEach
    void setUp() {
        rest   = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        client = new WsepClient(rest, BASE_URL);
    }

    // ── handshake ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("handshake returns true when WSEP replies OK")
    void handshake_ok() {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string("action_type=handshake"))
                .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

        assertThat(client.handshake()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("handshake returns false on unexpected response body")
    void handshake_failure() {
        server.expect(requestTo(BASE_URL))
                .andRespond(withSuccess("NOT_OK", MediaType.TEXT_PLAIN));

        assertThat(client.handshake()).isFalse();
    }

    // ── pay ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("pay returns the parsed transaction id on success")
    void pay_success() {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action_type=pay")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("amount=1000")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("currency=USD")))
                .andRespond(withSuccess("12345", MediaType.TEXT_PLAIN));

        int txn = client.pay(samplePayment());

        assertThat(txn).isEqualTo(12345);
    }

    @Test
    @DisplayName("pay returns -1 when WSEP rejects the card")
    void pay_declined() {
        server.expect(requestTo(BASE_URL))
                .andRespond(withSuccess("-1", MediaType.TEXT_PLAIN));

        assertThat(client.pay(samplePayment())).isEqualTo(WsepClient.FAILURE);
    }

    @Test
    @DisplayName("pay returns -1 on a malformed (non-integer) response")
    void pay_malformed() {
        server.expect(requestTo(BASE_URL))
                .andRespond(withSuccess("garbage", MediaType.TEXT_PLAIN));

        assertThat(client.pay(samplePayment())).isEqualTo(WsepClient.FAILURE);
    }

    // ── refund ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refund returns 1 on success")
    void refund_success() {
        server.expect(requestTo(BASE_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action_type=refund")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("transaction_id=12345")))
                .andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

        assertThat(client.refund("12345")).isEqualTo(1);
    }

    @Test
    @DisplayName("refund returns -1 on failure")
    void refund_failure() {
        server.expect(requestTo(BASE_URL))
                .andRespond(withSuccess("-1", MediaType.TEXT_PLAIN));

        assertThat(client.refund("12345")).isEqualTo(WsepClient.FAILURE);
    }

    @Test
    @DisplayName("refund rejects blank transaction id without calling WSEP")
    void refund_blankId() {
        assertThatThrownBy(() -> client.refund("  "))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify(); // no requests made
    }

    // ── issue_ticket ────────────────────────────────────────────────────────

    @Test
    @DisplayName("issueTicket returns the code on a standing-zone success")
    void issueTicket_standingSuccess() {
        server.expect(requestTo(BASE_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action_type=issue_ticket")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("zone=Golden+Ring")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("quantity=2")))
                .andRespond(withSuccess("TIX-1a2b-3456", MediaType.TEXT_PLAIN));

        String code = client.issueTicket(
                WsepIssueTicketRequest.standing("849302", "EVT-9923", "Golden Ring", 2));

        assertThat(code).isEqualTo("TIX-1a2b-3456");
    }

    @Test
    @DisplayName("issueTicket sends is_seating=true + seats JSON for assigned-seating zones")
    void issueTicket_seatingSuccess() {
        server.expect(requestTo(BASE_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is_seating=true")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("seats=")))
                .andRespond(withSuccess("TIX-vip-1", MediaType.TEXT_PLAIN));

        String code = client.issueTicket(
                WsepIssueTicketRequest.seating("849302", "EVT-9923", "VIP",
                        "[{\"row\":4,\"seat\":12}]"));

        assertThat(code).isEqualTo("TIX-vip-1");
    }

    @Test
    @DisplayName("issueTicket returns null when WSEP responds with -1")
    void issueTicket_failure() {
        server.expect(requestTo(BASE_URL))
                .andRespond(withSuccess("-1", MediaType.TEXT_PLAIN));

        String code = client.issueTicket(
                WsepIssueTicketRequest.standing("c", "e", "z", 1));

        assertThat(code).isNull();
    }

    // ── cancel_ticket ───────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelTicket returns 1 on success")
    void cancelTicket_success() {
        server.expect(requestTo(BASE_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ticket_id=TIX-1a2b")))
                .andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

        assertThat(client.cancelTicket("TIX-1a2b")).isEqualTo(1);
    }

    @Test
    @DisplayName("cancelTicket returns -1 on failure")
    void cancelTicket_failure() {
        server.expect(requestTo(BASE_URL))
                .andRespond(withSuccess("-1", MediaType.TEXT_PLAIN));

        assertThat(client.cancelTicket("TIX-1a2b")).isEqualTo(WsepClient.FAILURE);
    }

    // ── transport errors ────────────────────────────────────────────────────

    @Test
    @DisplayName("HTTP 5xx surfaces as a WsepException so the saga can compensate")
    void serverError_wrapsAsWsepException() {
        server.expect(requestTo(BASE_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.handshake())
                .isInstanceOf(WsepException.class)
                .hasMessageContaining("WSEP transport error");
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static WsepPaymentRequest samplePayment() {
        return new WsepPaymentRequest("1000", "USD",
                "2222333344445555", 4, 2030,
                "Israel Israelovice", "262", "20444444");
    }
}
