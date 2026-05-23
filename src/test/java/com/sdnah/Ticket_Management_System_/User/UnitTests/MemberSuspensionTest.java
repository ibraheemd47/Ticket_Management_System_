package com.sdnah.Ticket_Management_System_.User.UnitTests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Member;

/**
 * Pure domain tests for the Member suspension API (issue #94).
 * Boundary checks on {@code isSuspended()}: temporary expiry, permanent flag,
 * unsuspend restoration.
 */
class MemberSuspensionTest {

    @Test
    @DisplayName("Fresh member is not suspended")
    void freshMember_NotSuspended() {
        Member m = new Member("m-1", "u", "h");
        assertThat(m.isSuspended()).isFalse();
        assertThat(m.isSuspendedPermanently()).isFalse();
        assertThat(m.getSuspendedUntil()).isNull();
    }

    @Test
    @DisplayName("suspend(until) sets the temporary fields and isSuspended() returns true before expiry")
    void temporarySuspension_ActiveBeforeExpiry() {
        Member m = new Member("m-1", "u", "h");
        LocalDateTime until = LocalDateTime.now().plusHours(1);

        m.suspend(until);

        assertThat(m.isSuspended()).isTrue();
        assertThat(m.isSuspendedPermanently()).isFalse();
        assertThat(m.getSuspendedUntil()).isEqualTo(until);
        assertThat(m.getSuspensionStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("Temporary suspension expires automatically after the deadline")
    void temporarySuspension_ExpiresAutomatically() {
        Member m = new Member("m-1", "u", "h");
        m.suspend(LocalDateTime.now().minusSeconds(1)); // already past

        assertThat(m.isSuspended()).isFalse();
    }

    @Test
    @DisplayName("suspend(null) is treated as permanent suspension")
    void suspendNull_PermanentSuspension() {
        Member m = new Member("m-1", "u", "h");
        m.suspend(null);
        assertThat(m.isSuspendedPermanently()).isTrue();
        assertThat(m.isSuspended()).isTrue();
        assertThat(m.getSuspendedUntil()).isNull();
    }

    @Test
    @DisplayName("suspendPermanently flips the permanent flag and clears suspendedUntil")
    void suspendPermanently_SetsFlag() {
        Member m = new Member("m-1", "u", "h");
        m.suspendPermanently();
        assertThat(m.isSuspendedPermanently()).isTrue();
        assertThat(m.isSuspended()).isTrue();
        assertThat(m.getSuspendedUntil()).isNull();
    }

    @Test
    @DisplayName("unsuspend clears both temporary and permanent state")
    void unsuspend_ClearsBothPaths() {
        Member m = new Member("m-1", "u", "h");
        m.suspend(LocalDateTime.now().plusHours(3));
        m.unsuspend();
        assertThat(m.isSuspended()).isFalse();
        assertThat(m.getSuspendedUntil()).isNull();
        assertThat(m.getSuspensionStartedAt()).isNull();

        m.suspendPermanently();
        m.unsuspend();
        assertThat(m.isSuspended()).isFalse();
        assertThat(m.isSuspendedPermanently()).isFalse();
    }

    @Test
    @DisplayName("Permanent flag wins over a non-null suspendedUntil — isSuspended()=true even past expiry")
    void permanentFlag_TakesPrecedence() {
        Member m = new Member("m-1", "u", "h");
        m.suspend(LocalDateTime.now().minusDays(1)); // would expire
        m.suspendPermanently();                       // but permanent

        assertThat(m.isSuspended()).isTrue();
    }
}
