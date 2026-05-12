package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LIFO saga runner. Steps are submitted in business order; once a step's
 * {@code execute} returns normally it is pushed onto the completed stack. If a
 * later step throws, the runner walks the stack in reverse and invokes
 * {@code compensate} on each previously-completed step before rethrowing.
 *
 * Single-use — construct a new instance per business transaction.
 */
public final class OrderSaga {

    private static final Logger log = LoggerFactory.getLogger(OrderSaga.class);

    private final List<CompensableStep> completed = new ArrayList<>();
    private boolean rolledBack = false;

    /**
     * Run a step. On exception, the runner rolls back every previously
     * completed step (LIFO, best-effort) and rethrows the original exception.
     */
    public OrderSaga run(CompensableStep step) {
        if (rolledBack) {
            throw new IllegalStateException("saga has already rolled back");
        }
        try {
            step.execute();
            completed.add(step);
            return this;
        } catch (RuntimeException ex) {
            log.error("saga step '{}' failed: {} — rolling back", step.name(), ex.getMessage());
            rollback();
            throw ex;
        }
    }

    /** Manually trigger compensation of every completed step. */
    public void rollback() {
        if (rolledBack) return;
        rolledBack = true;
        for (int i = completed.size() - 1; i >= 0; i--) {
            CompensableStep step = completed.get(i);
            try {
                step.compensate();
                log.info("compensated step '{}'", step.name());
            } catch (RuntimeException ex) {
                log.error("compensation for step '{}' failed: {}",
                        step.name(), ex.getMessage(), ex);
            }
        }
        completed.clear();
    }

    public boolean isRolledBack() {
        return rolledBack;
    }

    public int completedStepCount() {
        return completed.size();
    }
}
