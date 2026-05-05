package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import com.sdnah.Ticket_Management_System_.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.DTOs.OrderDTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.OrderPolicyDomainService;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PolicyRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;

class ActiveOrderServiceTest {

        @Mock
        private ActiveOrderRepository orderRepo;

        @Mock
        private PurchaseRepository purchaseRepo;

        @Mock
        private PaymentTransactionRepository txRepo;

        @Mock
        private PaymentService paymentService;

        @Mock
        private ITicketSupplierGateway ticketGateway;

        @Mock
        private TicketRepository ticketRepository;

        @Mock
        private PolicyRepository policyRepository;

        @Mock
        private OrderPolicyDomainService orderPolicyDomainService;

        @Mock
        private IrepresnteUserService represnteUserService;

        private ActiveOrderService service;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);

                service = new ActiveOrderService(
                                orderRepo,
                                purchaseRepo,
                                txRepo,
                                paymentService,
                                ticketGateway,
                                ticketRepository,
                                policyRepository,
                                orderPolicyDomainService,
                                represnteUserService);
        }

        @Test
        @DisplayName("Given available tickets, when reserving tickets, then active order is created")
        void reserveTickets_shouldCreateOrder_whenTicketsAvailable() {
                // Arrange
                String userToken = "token-123";
                String buyerId = "buyer1";
                UUID eventId = UUID.randomUUID();

                UUID ticketId = UUID.randomUUID();

                SeatRequest seat = new SeatRequest(
                                ticketId.toString(),
                                1L,
                                UUID.randomUUID(),
                                new BigDecimal("50"));

                when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
                when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());
                when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(false);
                when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

                when(policyRepository.findPurchasePolicyByEventId(eventId)).thenReturn(null);
                when(policyRepository.findDiscountPolicyByEventId(eventId)).thenReturn(null);

                // Act
                OrderDTO result = service.reserveTickets(userToken, eventId, List.of(seat));

                // Assert
                assertEquals(buyerId, result.getbuyerId());
                assertEquals(eventId, result.getEventId());

                verify(represnteUserService).requireMemberId(userToken);
                verify(orderRepo).findActiveOrder(buyerId, eventId);
                verify(orderRepo).isTicketLocked(ticketId.toString());
                verify(orderRepo, times(2)).save(any(ActiveOrder.class));
                verify(orderPolicyDomainService).validatePurchasePolicy(any(ActiveOrder.class), any());
                verify(orderPolicyDomainService).applyDiscountPolicy(any(ActiveOrder.class), any(), any());
        }

        @Test
        @DisplayName("Given active order already exists, when reserving tickets, then exception is thrown")
        void reserveTickets_shouldThrow_whenActiveOrderExists() {
                // Arrange
                String userToken = "token-123";
                String buyerId = "buyer1";
                UUID eventId = UUID.randomUUID();

                when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
                when(orderRepo.findActiveOrder(buyerId, eventId))
                                .thenReturn(Optional.of(new ActiveOrder(buyerId, eventId, 10)));

                // Act + Assert
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                                () -> service.reserveTickets(userToken, eventId, List.of()));

                assertEquals("Active order already exists", ex.getMessage());

                verify(represnteUserService).requireMemberId(userToken);
                verify(orderRepo).findActiveOrder(buyerId, eventId);
        }

        @Test
        @DisplayName("Given ticket already locked, when reserving tickets, then exception is thrown")
        void reserveTickets_shouldThrow_whenTicketAlreadyLocked() {
                // Arrange
                String userToken = "token-123";
                String buyerId = "buyer1";
                UUID eventId = UUID.randomUUID();

                UUID ticketId = UUID.randomUUID();

                SeatRequest seat = new SeatRequest(
                                ticketId.toString(),
                                1L,
                                UUID.randomUUID(),
                                new BigDecimal("50"));

                when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
                when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());
                when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(true);

                // Act + Assert
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                                () -> service.reserveTickets(userToken, eventId, List.of(seat)));

                assertEquals("Ticket already reserved: " + ticketId, ex.getMessage());

                verify(represnteUserService).requireMemberId(userToken);
                verify(orderRepo).isTicketLocked(ticketId.toString());
        }

        @Test
        @DisplayName("Given repository save fails, when reserving tickets, then exception is propagated")
        void reserveTickets_shouldPropagateException_whenRepositorySaveFails() {
                // Arrange
                String userToken = "token-123";
                String buyerId = "buyer1";
                UUID eventId = UUID.randomUUID();

                UUID ticketId = UUID.randomUUID();

                SeatRequest seat = new SeatRequest(
                                ticketId.toString(),
                                1L,
                                UUID.randomUUID(),
                                new BigDecimal("50"));

                when(represnteUserService.requireMemberId(userToken)).thenReturn(buyerId);
                when(orderRepo.findActiveOrder(buyerId, eventId)).thenReturn(Optional.empty());
                when(orderRepo.isTicketLocked(ticketId.toString())).thenReturn(false);

                doThrow(new DataIntegrityViolationException("duplicate"))
                                .when(orderRepo).save(any(ActiveOrder.class));

                // Act + Assert
                assertThrows(DataIntegrityViolationException.class,
                                () -> service.reserveTickets(userToken, eventId, List.of(seat)));
        }
}