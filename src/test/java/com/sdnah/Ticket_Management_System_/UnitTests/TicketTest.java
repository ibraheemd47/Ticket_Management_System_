package com.sdnah.Ticket_Management_System_.UnitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Area;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class TicketTest {

    private ticket testTicket;
    private final UUID TICKET_ID = UUID.randomUUID();
    private final UUID SHOW_ID = UUID.randomUUID();
    private final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        // Mocking Area for the constructor
        Area mockArea = new Area("Bernabue Stadium"); 
        mockArea.setName("VIP Lounge");
        
        // Initialize an available ticket
        testTicket = new ticket(TICKET_ID, SHOW_ID, null, mockArea, new Date(), new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Ticket should start with AVAILABLE status")
    void testInitialStatus() {
        assertThat(testTicket.getStatus()).isEqualTo(ticket.TicketStatus.AVAILABLE);
        assertThat(testTicket.getOwnerId()).isNull();
    }

    // @Test
    // @DisplayName("Should successfully lock an available ticket in cart")
    // void testLockInCartSuccess() {
    //     boolean result = testTicket.lockInCart(USER_ID);
        
    //     assertThat(result).isTrue();
    //     assertThat(testTicket.getStatus()).isEqualTo(ticket.TicketStatus.LOCKED_IN_CART);
    //     assertThat(testTicket.getOwnerId()).isEqualTo(USER_ID);
    // }

    // @Test
    // @DisplayName("Should fail to lock a ticket that is already purchased")
    // void testLockInCartFailure() {
    //     // Manually move ticket to a state where it shouldn't be lockable
    //     testTicket.lockInCart(USER_ID);
    //     testTicket.purchase(USER_ID);
        
    //     boolean result = testTicket.lockInCart("other-user");
        
    //     assertThat(result).isFalse();
    //     assertThat(testTicket.getStatus()).isEqualTo(ticket.TicketStatus.PURCHASED);
    // }

    // @Test
    // @DisplayName("Purchase should only succeed if ticket is LOCKED_IN_CART")
    // void testPurchaseFlow() {
    //     // Try purchasing directly (should fail)
    //     assertThat(testTicket.purchase(USER_ID)).isFalse();

    //     // Lock it first
    //     testTicket.lockInCart(USER_ID);
        
    //     // Try purchasing (should succeed)
    //     boolean result = testTicket.purchase(USER_ID);
        
    //     assertThat(result).isTrue();
    //     assertThat(testTicket.getStatus()).isEqualTo(ticket.TicketStatus.PURCHASED);
    // }

    // @Test
    // @DisplayName("Unlock should make ticket available again")
    // void testUnlockFromCart() {
    //     testTicket.lockInCart(USER_ID);
    //     boolean result = testTicket.unlockFromCart();
        
    //     assertThat(result).isTrue();
    //     assertThat(testTicket.getStatus()).isEqualTo(ticket.TicketStatus.AVAILABLE);
    //     assertThat(testTicket.getOwnerId()).isNull();
    // }

    // @Test
    // @DisplayName("Scanning should only work for purchased tickets")
    // void testScanTicket() {
    //     // Try scanning available ticket (fail)
    //     assertThat(testTicket.scan()).isFalse();

    //     // Purchase it
    //     testTicket.lockInCart(USER_ID);
    //     testTicket.purchase(USER_ID);
        
    //     // Scan it (success)
    //     assertThat(testTicket.scan()).isTrue();
    //     assertThat(testTicket.getStatus()).isEqualTo(ticket.TicketStatus.SCANNED);
    // }

    // @Test
    // @DisplayName("Cancel should return purchased ticket to available pool")
    // void testCancelTicket() {
    //     testTicket.lockInCart(USER_ID);
    //     testTicket.purchase(USER_ID);
        
    //     boolean result = testTicket.cancel();
        
    //     assertThat(result).isTrue();
    //     assertThat(testTicket.getStatus()).isEqualTo(ticket.TicketStatus.AVAILABLE);
    //     assertThat(testTicket.getOwnerId()).isNull();
    // }
}
