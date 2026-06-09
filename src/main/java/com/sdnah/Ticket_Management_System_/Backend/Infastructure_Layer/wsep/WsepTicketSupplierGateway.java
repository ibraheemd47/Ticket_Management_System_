package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.Ticketcode;

/**
 * Adapter that fulfils {@link ITicketSupplierGateway} by delegating to
 * {@link WsepClient#issueTicket}. Groups order items by their area (the
 * "zone" in WSEP parlance) so each WSEP call covers items belonging to the
 * same zone, then explodes the returned ticket code back into one
 * {@link Ticketcode} per input item — which is what {@code OrderSaga} expects.
 *
 * <p>NOT a {@code @Component} yet — see {@link WsepPaymentGateway} for the
 * rationale (Phase 2 wires this in via a property switch).
 */
public class WsepTicketSupplierGateway implements ITicketSupplierGateway {

    private static final Logger log = LoggerFactory.getLogger(WsepTicketSupplierGateway.class);

    private final WsepClient client;

    public WsepTicketSupplierGateway(WsepClient client) {
        this.client = client;
    }

    @Override
    public List<Ticketcode> issueTickets(UUID purchaseId, List<OrderItem> items) {
        if (purchaseId == null) throw new IllegalArgumentException("purchaseId required");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("items required");

        // Group by area so each WSEP call issues tickets for one zone at a time.
        Map<UUID, List<OrderItem>> byArea = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getAreaId,
                        HashMap::new, Collectors.toList()));

        List<Ticketcode> out = new ArrayList<>(items.size());
        for (Map.Entry<UUID, List<OrderItem>> entry : byArea.entrySet()) {
            UUID areaId = entry.getKey();
            List<OrderItem> areaItems = entry.getValue();
            WsepIssueTicketRequest req = buildRequest(purchaseId, areaId, areaItems);

            String code = client.issueTicket(req);
            if (code == null) {
                log.error("WSEP failed to issue tickets purchaseId={} areaId={} count={}",
                        purchaseId, areaId, areaItems.size());
                throw new WsepException(
                        "WSEP refused to issue tickets for area " + areaId);
            }

            // WSEP returns a single ticket id per request even when quantity > 1.
            // We currently lack a way to ask for one code per item; derive
            // per-item codes deterministically from the base code so each
            // OrderItem still maps to a unique Ticketcode (the saga assumes
            // one per item). When WSEP later supports per-item codes this
            // helper is the only place to change.
            for (int i = 0; i < areaItems.size(); i++) {
                OrderItem it = areaItems.get(i);
                String perItem = areaItems.size() == 1 ? code : code + "-" + (i + 1);
                out.add(new Ticketcode(perItem,
                        "QR-" + purchaseId + "-" + it.getTicketId()));
            }
            log.info("WSEP issued tickets purchaseId={} areaId={} baseCode={} count={}",
                    purchaseId, areaId, code, areaItems.size());
        }
        return out;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static WsepIssueTicketRequest buildRequest(UUID purchaseId, UUID areaId,
                                                       List<OrderItem> areaItems) {
        String customerId = purchaseId.toString();        // best identifier we have
        String eventId    = areaId.toString();            // WSEP needs a stable id per "event"
        String zone       = areaId.toString();

        boolean assignedSeating = areaItems.stream().anyMatch(OrderItem::isSeated);
        if (assignedSeating) {
            String seatsJson = areaItems.stream()
                    .map(it -> "{\"row\":0,\"seat\":" + it.getSeatId() + "}")
                    .collect(Collectors.joining(",", "[", "]"));
            return WsepIssueTicketRequest.seating(customerId, eventId, zone, seatsJson);
        }
        return WsepIssueTicketRequest.standing(customerId, eventId, zone, areaItems.size());
    }
}
