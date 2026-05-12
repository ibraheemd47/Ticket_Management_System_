package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

/**
 * One step of a saga: a forward action paired with the inverse action that
 * undoes it. The saga runner guarantees {@link #compensate()} is invoked only
 * if {@link #execute()} previously succeeded for this step, and that
 * compensations of earlier-completed steps run when a later step fails.
 *
 * Implementations should make {@code compensate} idempotent — external side
 * effects (refunds, voids) often arrive at the gateway more than once.
 */
public interface CompensableStep {

    void execute();

    void compensate();

    default String name() {
        return getClass().getSimpleName();
    }
}
