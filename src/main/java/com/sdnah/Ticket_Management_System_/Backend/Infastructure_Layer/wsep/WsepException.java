package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep;

/**
 * Raised when the {@link WsepClient} can't reach WSEP (network timeout, DNS
 * failure, 5xx, etc). Distinct from a "WSEP returned -1" business failure —
 * those just surface as the documented sentinel value.
 *
 * <p>Callers in the order saga should treat this as a compensation trigger,
 * not a crash. Idempotent retries are appropriate.
 */
public class WsepException extends RuntimeException {

    public WsepException(String message, Throwable cause) {
        super(message, cause);
    }

    public WsepException(String message) {
        super(message);
    }
}
