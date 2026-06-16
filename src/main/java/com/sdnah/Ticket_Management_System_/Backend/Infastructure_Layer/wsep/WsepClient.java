package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin HTTP wrapper over the WSEP 2026 external systems endpoint. One method
 * per documented action ({@code handshake}, {@code pay}, {@code refund},
 * {@code issue_ticket}, {@code cancel_ticket}). Every method maps the
 * raw form-encoded response back to a small Java type so the gateway adapter
 * doesn't have to deal with strings / ints / network exceptions itself.
 *
 * <p>Per the WSEP spec the service returns:
 * <ul>
 *   <li>{@code handshake}: the literal {@code "OK"}</li>
 *   <li>{@code pay}: an integer transaction id in [10000, 100000] on success,
 *       or {@code -1} on failure</li>
 *   <li>{@code refund}: {@code 1} on success, {@code -1} on failure</li>
 *   <li>{@code issue_ticket}: a ticket code string (e.g. {@code TIX-1a2b-3456})
 *       on success, or {@code "-1"} on failure</li>
 *   <li>{@code cancel_ticket}: {@code 1} on success, {@code -1} on failure</li>
 * </ul>
 *
 * <p>Transport-level errors surface as {@link WsepException} so the caller can
 * wrap saga rollbacks around them; transient network problems do not crash
 * the JVM.
 */
public class WsepClient {

    private static final Logger log = LoggerFactory.getLogger(WsepClient.class);

    /** Sentinel returned by WSEP on any business-level failure. */
    public static final int FAILURE = -1;

    private final RestTemplate rest;
    private final String baseUrl;

    /**
     * Production constructor — builds a {@link RestTemplate} with connect/read
     * timeouts so a stuck WSEP instance doesn't pin a request thread forever.
     */
    public WsepClient(WsepProperties props, RestTemplateBuilder builder) {
        this(builder
                .setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .build(),
             props.getBaseUrl());
    }

    /**
     * Test-friendly constructor — let callers wire any {@link RestTemplate}
     * (e.g. one bound to {@code MockRestServiceServer}).
     */
    public WsepClient(RestTemplate rest, String baseUrl) {
        this.rest    = Objects.requireNonNull(rest, "rest");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
    }

    // ── handshake ───────────────────────────────────────────────────────────

    /** @return {@code true} iff WSEP returned the literal "OK". */
    public boolean handshake() {
        String body = postForm(Map.of("action_type", "handshake"));
        boolean ok = "OK".equalsIgnoreCase(strip(body));
        if (!ok) {
            log.warn("WSEP handshake returned unexpected body: {}", body);
        }
        return ok;
    }

    // ── pay ─────────────────────────────────────────────────────────────────

    /**
     * Charge a payment via WSEP.
     * @return transaction id in [10000, 100000] on success, {@link #FAILURE} on failure.
     */
    public int pay(WsepPaymentRequest req) {
        Objects.requireNonNull(req, "req");
        Map<String, String> params = new HashMap<>();
        params.put("action_type",  "pay");
        params.put("amount",       req.amount());
        params.put("currency",     req.currency());
        params.put("card_number",  req.cardNumber());
        params.put("month",        String.valueOf(req.month()));
        params.put("year",         String.valueOf(req.year()));
        params.put("holder",       req.holder());
        params.put("cvv",          req.cvv());
        params.put("id",           req.holderId());

        return parseIntOrFailure(postForm(params));
    }

    // ── refund ──────────────────────────────────────────────────────────────

    /**
     * Refund a previously-successful WSEP transaction.
     * @return {@code 1} on success, {@link #FAILURE} on failure.
     */
    public int refund(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId required");
        }
        String body = postForm(Map.of(
                "action_type",    "refund",
                "transaction_id", transactionId));
        return parseIntOrFailure(body);
    }

    // ── issue_ticket ────────────────────────────────────────────────────────

    /**
     * Issue a ticket code via WSEP.
     * @return ticket code string on success (e.g. "TIX-1a2b-3456"), or
     *         {@code null} on failure (WSEP returns the string "-1").
     */
    public String issueTicket(WsepIssueTicketRequest req) {
        Objects.requireNonNull(req, "req");
        Map<String, String> params = new HashMap<>();
        params.put("action_type", "issue_ticket");
        params.put("customer_id", req.customerId());
        params.put("event_id",    req.eventId());
        params.put("zone",        req.zone());
        if (req.seatsJson() != null && !req.seatsJson().isBlank()) {
            // Assigned-seating zone.
            params.put("is_seating", "true");
            params.put("seats",      req.seatsJson());
        } else {
            // General-admission zone — quantity is required.
            params.put("quantity", String.valueOf(req.quantity()));
        }

        String body = strip(postForm(params));
        if (body.isBlank() || "-1".equals(body)) {
            return null;
        }
        return body;
    }

    // ── cancel_ticket ───────────────────────────────────────────────────────

    /**
     * Cancel a previously-issued ticket via WSEP.
     * @return {@code 1} on success, {@link #FAILURE} on failure.
     */
    public int cancelTicket(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId required");
        }
        String body = postForm(Map.of(
                "action_type", "cancel_ticket",
                "ticket_id",   ticketId));
        return parseIntOrFailure(body);
    }

    // ── transport ───────────────────────────────────────────────────────────

    private String postForm(Map<String, String> params) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        try {
            String response = rest.postForObject(baseUrl, entity, String.class);
            return response == null ? "" : response;
        } catch (RestClientException ex) {
            log.error("WSEP call failed: action_type={}, error={}",
                    params.get("action_type"), ex.toString());
            throw new WsepException("WSEP transport error: " + ex.getMessage(), ex);
        }
    }

    private static int parseIntOrFailure(String body) {
        String clean = strip(body);
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException nfe) {
            log.warn("WSEP returned non-integer body: '{}'", body);
            return FAILURE;
        }
    }

    /** Trim whitespace + trailing newlines that WSEP sometimes appends. */
    private static String strip(String s) {
        if (s == null) return "";
        return s.trim();
    }
}
