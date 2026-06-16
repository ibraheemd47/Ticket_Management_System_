package com.sdnah.Ticket_Management_System_.wsep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepClient;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepException;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepIssueTicketRequest;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepTicketSupplierGateway;

/**
 * Unit tests for {@link WsepTicketSupplierGateway}'s mapping + zone grouping
 * logic. {@link WsepClient} is mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WsepTicketSupplierGateway — Adapter mapping")
class WsepTicketSupplierGatewayTest {

    @Mock private WsepClient client;

    private WsepTicketSupplierGateway gateway;

    private final UUID purchaseId = UUID.randomUUID();
    private final UUID areaA      = UUID.randomUUID();
    private final UUID areaB      = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        gateway = new WsepTicketSupplierGateway(client);
    }

    private OrderItem standing(UUID areaId) {
        return new OrderItem("TKT-" + UUID.randomUUID(), null, areaId, new BigDecimal("50"));
    }

    private OrderItem seated(UUID areaId, long seatId) {
        return new OrderItem("TKT-" + UUID.randomUUID(), seatId, areaId, new BigDecimal("80"));
    }

    // ── standing-only purchase ──────────────────────────────────────────────

    @Test
    @DisplayName("standing items get one issue_ticket call with quantity=n; one code returned per item")
    void singleZoneStanding_returnsOneCodePerItem() {
        when(client.issueTicket(any())).thenReturn("TIX-base");

        List<OrderItem> items = List.of(standing(areaA), standing(areaA), standing(areaA));

        List<Ticketcode> codes = gateway.issueTickets(purchaseId, items);

        assertThat(codes).hasSize(3);
        // Codes are derived from the base; first item gets the base code,
        // others get suffixes so they are still unique.
        assertThat(codes.get(0).getCode()).isEqualTo("TIX-base-1");
        assertThat(codes.get(1).getCode()).isEqualTo("TIX-base-2");
        assertThat(codes.get(2).getCode()).isEqualTo("TIX-base-3");

        ArgumentCaptor<WsepIssueTicketRequest> captor =
                ArgumentCaptor.forClass(WsepIssueTicketRequest.class);
        Mockito.verify(client).issueTicket(captor.capture());
        WsepIssueTicketRequest sent = captor.getValue();
        assertThat(sent.seatsJson()).isNull();
        assertThat(sent.quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("single item returns the WSEP code unchanged (no suffix)")
    void singleStandingItem_returnsBaseCodeAsIs() {
        when(client.issueTicket(any())).thenReturn("TIX-only");

        List<Ticketcode> codes = gateway.issueTickets(purchaseId, List.of(standing(areaA)));

        assertThat(codes).singleElement()
                .satisfies(t -> assertThat(t.getCode()).isEqualTo("TIX-only"));
    }

    // ── seating purchase ────────────────────────────────────────────────────

    @Test
    @DisplayName("seated items emit a seats JSON array, not a quantity")
    void seatingZone_sendsSeatsJson() {
        when(client.issueTicket(any())).thenReturn("TIX-vip");

        List<OrderItem> items = List.of(seated(areaA, 12L), seated(areaA, 13L));

        gateway.issueTickets(purchaseId, items);

        ArgumentCaptor<WsepIssueTicketRequest> captor =
                ArgumentCaptor.forClass(WsepIssueTicketRequest.class);
        Mockito.verify(client).issueTicket(captor.capture());
        WsepIssueTicketRequest sent = captor.getValue();
        assertThat(sent.seatsJson()).isNotNull();
        assertThat(sent.seatsJson()).contains("\"seat\":12").contains("\"seat\":13");
    }

    // ── multiple zones ──────────────────────────────────────────────────────

    @Test
    @DisplayName("items spanning multiple areas trigger one WSEP call per area")
    void multipleAreas_oneCallPerArea() {
        when(client.issueTicket(any()))
                .thenReturn("TIX-a")   // for areaA
                .thenReturn("TIX-b");  // for areaB

        List<OrderItem> items = new ArrayList<>();
        items.add(standing(areaA));
        items.add(standing(areaB));

        List<Ticketcode> codes = gateway.issueTickets(purchaseId, items);

        assertThat(codes).hasSize(2);
        Mockito.verify(client, Mockito.times(2)).issueTicket(any());
    }

    // ── failure path ────────────────────────────────────────────────────────

    @Test
    @DisplayName("WSEP returning null surfaces as WsepException so the saga can roll back")
    void issueTicketFailure_throwsWsepException() {
        when(client.issueTicket(any())).thenReturn(null);

        assertThatThrownBy(() ->
                gateway.issueTickets(purchaseId, List.of(standing(areaA))))
                .isInstanceOf(WsepException.class)
                .hasMessageContaining("refused");
    }

    @Test
    @DisplayName("rejects null / empty input")
    void issueTickets_validatesInput() {
        assertThatThrownBy(() -> gateway.issueTickets(null, List.of(standing(areaA))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> gateway.issueTickets(purchaseId, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
