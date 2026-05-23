package com.sdnah.Ticket_Management_System_.Event.UnitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.show_type;

import java.util.Date;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventTest {

    private Event event;
    private final Long OWNER_ID = 100L;
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private final Long MANAGER_ID = 200L;
    private final Long UNAUTHORIZED_USER = 999L;

    @BeforeEach
    void setUp() {
        // Initialize a fresh event before each test
        event = new Event("Summer Fest", show_type.CONFERENCE, COMPANY_ID, OWNER_ID);
    }

    @Test
    @DisplayName("Should initialize event with owner as the first manager")
    void testInitialState() {
        assertThat(event.getName()).isEqualTo("Summer Fest");
        assertThat(event.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(event.getManagerIds()).contains(OWNER_ID);
    }

    @Test
    @DisplayName("Owner should be able to add a new manager")
    void testAddManagerSuccess() {
        event.addManager(MANAGER_ID, OWNER_ID);
        assertThat(event.getManagerIds()).contains(MANAGER_ID);
    }

    @Test
    @DisplayName("Non-owner should not be able to add a manager")
    void testAddManagerUnauthorized() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            event.addManager(MANAGER_ID, UNAUTHORIZED_USER);
        });
        assertThat(exception.getMessage()).contains("Only the owner can add managers");
    }

    @Test
    @DisplayName("Manager should be able to add a show")
    void testAddShowSuccess() {
        // First add a manager
        event.addManager(MANAGER_ID, OWNER_ID);

        // Mock a show object (assuming show has a basic constructor)
        show newShow = new show();

        event.addShow(newShow, MANAGER_ID);

        assertThat(event.getShows()).hasSize(1);
        assertThat(event.getShows().get(0)).isEqualTo(newShow);
    }

    @Test
    @DisplayName("Unauthorized user should not be able to add a show")
    void testAddShowUnauthorized() {
        show newShow = new show();
        assertThatThrownBy(() -> event.addShow(newShow, UNAUTHORIZED_USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only managers can add shows");
    }

    @Test
    @DisplayName("Owner should be able to delete event and clear lists")
    void testDeleteEvent() {
        event.addManager(MANAGER_ID, OWNER_ID);

        event.delete(OWNER_ID);

        assertThat(event.getShows()).isEmpty();
        assertThat(event.getManagerIds()).isEmpty();
    }

    @Test
    @DisplayName("Deleting event with wrong owner ID should fail")
    void testDeleteEventUnauthorized() {
        assertThatThrownBy(() -> event.delete(UNAUTHORIZED_USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only the owner can delete the event");
    }

    // ── Remove Show ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Manager should be able to remove a show")
    void testRemoveShowSuccess() {
        event.addManager(MANAGER_ID, OWNER_ID);
        show s = new show();
        event.addShow(s, MANAGER_ID);

        event.removeShow(s, MANAGER_ID);

        assertThat(event.getShows()).isEmpty();
    }

    @Test
    @DisplayName("Unauthorized user should not be able to remove a show")
    void testRemoveShowUnauthorized() {
        event.addManager(MANAGER_ID, OWNER_ID);
        show s = new show();
        event.addShow(s, MANAGER_ID);

        assertThatThrownBy(() -> event.removeShow(s, UNAUTHORIZED_USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only managers can remove shows");
    }

    // ── Remove Manager ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Owner should be able to remove a manager")
    void testRemoveManagerSuccess() {
        event.addManager(MANAGER_ID, OWNER_ID);
        assertThat(event.getManagerIds()).contains(MANAGER_ID);

        event.removeManager(MANAGER_ID, OWNER_ID);

        assertThat(event.getManagerIds()).doesNotContain(MANAGER_ID);
    }

    @Test
    @DisplayName("Non-owner should not be able to remove a manager")
    void testRemoveManagerUnauthorized() {
        event.addManager(MANAGER_ID, OWNER_ID);

        assertThatThrownBy(() -> event.removeManager(MANAGER_ID, UNAUTHORIZED_USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only the owner can remove managers");
    }

    @Test
    @DisplayName("Adding a duplicate manager should fail")
    void testAddDuplicateManager() {
        event.addManager(MANAGER_ID, OWNER_ID);

        assertThatThrownBy(() -> event.addManager(MANAGER_ID, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    @DisplayName("Removing a manager not assigned should fail")
    void testRemoveNonExistentManager() {
        assertThatThrownBy(() -> event.removeManager(MANAGER_ID, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not assigned to the event");
    }

    // ── Edit Methods ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edit event fields")
    class EditEventFields {

        @Test
        @DisplayName("Manager can edit event name")
        void testEditNameSuccess() {
            event.addManager(MANAGER_ID, OWNER_ID);
            event.editName("Winter Fest", MANAGER_ID);
            assertThat(event.getName()).isEqualTo("Winter Fest");
        }

        @Test
        @DisplayName("Unauthorized user cannot edit event name")
        void testEditNameUnauthorized() {
            assertThatThrownBy(() -> event.editName("Hack", UNAUTHORIZED_USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only managers can edit the event name");
        }

        @Test
        @DisplayName("Manager can edit event type")
        void testEditTypeSuccess() {
            event.addManager(MANAGER_ID, OWNER_ID);
            event.editType(show_type.FESTIVAL, MANAGER_ID);
            assertThat(event.getEventType()).isEqualTo(show_type.FESTIVAL);
        }

        @Test
        @DisplayName("Unauthorized user cannot edit event type")
        void testEditTypeUnauthorized() {
            assertThatThrownBy(() -> event.editType(show_type.FESTIVAL, UNAUTHORIZED_USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only managers can edit the event type");
        }

        @Test
        @DisplayName("Manager can edit event dates")
        void testEditDatesSuccess() {
            event.addManager(MANAGER_ID, OWNER_ID);
            Date start = new Date();
            Date end = new Date(start.getTime() + 86_400_000L);

            event.editDates(start, end, MANAGER_ID);

            assertThat(event.getStartDate()).isEqualTo(start);
            assertThat(event.getEndDate()).isEqualTo(end);
        }

        @Test
        @DisplayName("Unauthorized user cannot edit event dates")
        void testEditDatesUnauthorized() {
            assertThatThrownBy(() -> event.editDates(new Date(), new Date(), UNAUTHORIZED_USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only managers can edit the event dates");
        }

        @Test
        @DisplayName("Manager can edit event venue")
        void testEditVenueSuccess() {
            event.addManager(MANAGER_ID, OWNER_ID);
            event.editVenue("Yarkon Park", MANAGER_ID);
            assertThat(event.getVenue()).isEqualTo("Yarkon Park");
        }

        @Test
        @DisplayName("Unauthorized user cannot edit event venue")
        void testEditVenueUnauthorized() {
            assertThatThrownBy(() -> event.editVenue("Yarkon Park", UNAUTHORIZED_USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only managers can edit the event venue");
        }

        @Test
        @DisplayName("Manager can edit event description")
        void testEditDescriptionSuccess() {
            event.addManager(MANAGER_ID, OWNER_ID);
            event.editDescription("An outdoor festival", MANAGER_ID);
            assertThat(event.getDescription()).isEqualTo("An outdoor festival");
        }

        @Test
        @DisplayName("Unauthorized user cannot edit event description")
        void testEditDescriptionUnauthorized() {
            assertThatThrownBy(() -> event.editDescription("hack", UNAUTHORIZED_USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only managers can edit the event description");
        }
    }

    // ── Reviews ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Event reviews")
    class EventReviews {

        @Test
        @DisplayName("User can add a valid review")
        void testAddReviewSuccess() {
            UUID userId = UUID.randomUUID();
            event.addReview(userId, 4);
            assertThat(event.getReviews().get(userId)).isEqualTo(4);
        }

        @Test
        @DisplayName("Review rating below 1 should be rejected")
        void testAddReviewTooLow() {
            assertThatThrownBy(() -> event.addReview(UUID.randomUUID(), 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rating must be between 1 and 5");
        }

        @Test
        @DisplayName("Review rating above 5 should be rejected")
        void testAddReviewTooHigh() {
            assertThatThrownBy(() -> event.addReview(UUID.randomUUID(), 6))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rating must be between 1 and 5");
        }

        @Test
        @DisplayName("Multiple users can review the same event")
        void testMultipleReviews() {
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            event.addReview(user1, 5);
            event.addReview(user2, 2);
            assertThat(event.getReviews()).hasSize(2);
            assertThat(event.getReviews().get(user1)).isEqualTo(5);
            assertThat(event.getReviews().get(user2)).isEqualTo(2);
        }

        @Test
        @DisplayName("Second review from same user overwrites the first")
        void testReviewOverwrite() {
            UUID userId = UUID.randomUUID();
            event.addReview(userId, 3);
            event.addReview(userId, 5);
            assertThat(event.getReviews().get(userId)).isEqualTo(5);
            assertThat(event.getReviews()).hasSize(1);
        }
    }
}
