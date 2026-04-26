package com.sdnah.Ticket_Management_System_.UnitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.show_type;

import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventTest {

    private Event event;
    private final Long OWNER_ID = 100L;
    private final Long COMPANY_ID = 500L;
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
}
