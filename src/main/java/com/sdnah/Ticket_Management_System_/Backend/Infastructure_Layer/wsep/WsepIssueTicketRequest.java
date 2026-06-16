package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep;

/**
 * Form parameters for a WSEP {@code issue_ticket} request.
 *
 * <p>The WSEP spec accepts two zone styles:
 * <ul>
 *   <li>General admission / standing — supply {@code quantity} and leave
 *       {@code seatsJson} {@code null}.</li>
 *   <li>Assigned seating — supply {@code seatsJson} (the JSON-string
 *       variant, e.g. {@code [{"row":4,"seat":12}]}) and {@code quantity}
 *       is ignored.</li>
 * </ul>
 */
public record WsepIssueTicketRequest(
        String customerId,
        String eventId,
        String zone,
        int    quantity,
        String seatsJson) {

    /** Convenience constructor for general admission / standing zones. */
    public static WsepIssueTicketRequest standing(String customerId, String eventId,
                                                  String zone, int quantity) {
        return new WsepIssueTicketRequest(customerId, eventId, zone, quantity, null);
    }

    /** Convenience constructor for assigned seating zones. */
    public static WsepIssueTicketRequest seating(String customerId, String eventId,
                                                 String zone, String seatsJson) {
        return new WsepIssueTicketRequest(customerId, eventId, zone, 0, seatsJson);
    }
}
