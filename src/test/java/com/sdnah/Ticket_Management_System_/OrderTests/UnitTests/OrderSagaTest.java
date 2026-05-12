package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.CompensableStep;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderSaga;

class OrderSagaTest {

    private static CompensableStep step(String name, List<String> log, Runnable forward) {
        return new CompensableStep() {
            @Override public void execute()    { forward.run(); log.add("do:" + name); }
            @Override public void compensate() { log.add("undo:" + name); }
            @Override public String name()     { return name; }
        };
    }

    @Test
    @DisplayName("All steps succeed: no compensation runs and the saga is not marked rolled back")
    void allStepsSucceed_NoCompensationRuns() {
        List<String> log = new ArrayList<>();
        OrderSaga saga = new OrderSaga();

        saga.run(step("a", log, () -> {}))
            .run(step("b", log, () -> {}))
            .run(step("c", log, () -> {}));

        assertThat(log).containsExactly("do:a", "do:b", "do:c");
        assertThat(saga.isRolledBack()).isFalse();
        assertThat(saga.completedStepCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Middle step throws: prior steps compensate in LIFO order and exception bubbles up")
    void middleStepThrows_PriorStepsCompensateLifo() {
        List<String> log = new ArrayList<>();
        OrderSaga saga = new OrderSaga();
        saga.run(step("a", log, () -> {}));

        assertThatThrownBy(() ->
                saga.run(step("b", log, () -> { throw new IllegalStateException("boom"); })))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");

        assertThat(log).containsExactly("do:a", "undo:a");
        assertThat(saga.isRolledBack()).isTrue();
    }

    @Test
    @DisplayName("Compensation throws: other compensations still run (best-effort rollback)")
    void compensationThrows_OtherCompensationsStillRun() {
        List<String> log = new ArrayList<>();
        OrderSaga saga = new OrderSaga();

        saga.run(step("a", log, () -> {}));
        saga.run(new CompensableStep() {
            @Override public void execute()    { log.add("do:b"); }
            @Override public void compensate() { log.add("undo:b-fail"); throw new RuntimeException("boom-undo"); }
            @Override public String name()     { return "b"; }
        });

        assertThatThrownBy(() ->
                saga.run(step("c", log, () -> { throw new RuntimeException("trigger"); })))
            .isInstanceOf(RuntimeException.class);

        assertThat(log).containsExactly("do:a", "do:b", "undo:b-fail", "undo:a");
    }

    @Test
    @DisplayName("After rollback, further run() calls are rejected")
    void afterRollback_FurtherRunIsRejected() {
        OrderSaga saga = new OrderSaga();
        saga.rollback();

        assertThatThrownBy(() -> saga.run(step("x", new ArrayList<>(), () -> {})))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already rolled back");
    }
}
