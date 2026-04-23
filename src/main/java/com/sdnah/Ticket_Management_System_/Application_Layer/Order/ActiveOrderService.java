package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderItemDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.SeatRequest;

public class ActiveOrderService {
    private static final Logger logger = LoggerFactory.getLogger(ActiveOrderService.class);
    private static final int TTL_MINUTES = 10;
    private final IOrderRepository orderRepository;
    private final PaymentService paymentService;
    private final ITicketSupplierGateway ticketGateway;
    private final PolicyService policyService;

    public ActiveOrderService(IOrderRepository orderRepository,
            PaymentService paymentService,
            ITicketSupplierGateway ticketGateway) {
        if (orderRepository == null)
            throw new IllegalArgumentException("orderRepository required");
        if (paymentService == null)
            throw new IllegalArgumentException("paymentService required");
        if (ticketGateway == null)
            throw new IllegalArgumentException("ticketGateway required");
        if (policyService == null)
            throw new IllegalArgumentException("policyService required");
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.ticketGateway = ticketGateway;
        this.policyService = policyService;
    }

    public OrderDTO reserveTickets(String buyerId, int eventId, List<SeatRequest> seats) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public OrderDTO removeFromOrder(UUID orderId, UUID itemId, String buyerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public PurchaseDTO checkout(UUID orderId, String buyerId, PaymentDetailsDTO paymentDTO) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public OrderDTO getActiveOrder(String buyerId, int eventId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<PurchaseDTO> getPurchaseHistory(String buyerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public OrderDTO applyCoupon(UUID orderId, String buyerId, String couponCode) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private ActiveOrder findValidOrder(UUID orderId, String buyerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}

// package application.order;

// import application.order.dto.*;
// import domain.order.*;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.UUID;
// import java.util.stream.Collectors;

// /**
// * ActiveOrderService — application layer.
// * Corresponds to active_order_service in the diagram.
// * NO domain logic — delegates to domain objects.
// * Returns DTOs only — never domain objects.
// * [application layer]
// */
// public class ActiveOrderService {

// private static final Logger logger =
// LoggerFactory.getLogger(ActiveOrderService.class);

// private static final int TTL_MINUTES = 10;

// private final IOrderRepository orderRepository;
// private final PaymentService paymentService;
// private final ITicketSupplierGateway ticketGateway;

// public ActiveOrderService(IOrderRepository orderRepository,
// PaymentService paymentService,
// ITicketSupplierGateway ticketGateway) {
// if (orderRepository == null) throw new
// IllegalArgumentException("orderRepository required");
// if (paymentService == null) throw new
// IllegalArgumentException("paymentService required");
// if (ticketGateway == null) throw new IllegalArgumentException("ticketGateway
// required");
// this.orderRepository = orderRepository;
// this.paymentService = paymentService;
// this.ticketGateway = ticketGateway;
// }

// public OrderDTO reserveTickets(String buyerId,
// int eventId,
// List<SeatRequest> seats) {
// logger.info("USE-CASE reserveTickets | buyerId={} eventId={} count={}",
// buyerId, eventId, seats.size());

// orderRepository.findActiveOrder(buyerId, eventId).ifPresent(o -> {
// logger.warn("reserveTickets REJECTED | duplicate | buyerId={} eventId={}",
// buyerId, eventId);
// throw new IllegalStateException(
// "Active order already exists: buyer=" + buyerId + " event=" + eventId);
// });

// ActiveOrder order = new ActiveOrder(buyerId, eventId, TTL_MINUTES);

// for (SeatRequest seat : seats) {
// Lock lock = new Lock(
// seat.ticketId(), // String
// buyerId,
// LocalDateTime.now().plusMinutes(TTL_MINUTES)
// );

// boolean acquired = orderRepository.acquireLock(lock);
// if (!acquired) {
// logger.warn("reserveTickets REJECTED | ticket locked | ticketId={}",
// seat.ticketId());
// order.getItems().forEach(i ->
// orderRepository.releaseLock(i.getLockResourceId()));
// throw new IllegalStateException(
// "Ticket already reserved: " + seat.ticketId());
// }

// OrderItem item = new OrderItem(
// seat.ticketId(), // String
// seat.seatId(),
// seat.areaId(),
// seat.price()
// );
// item.setLock(lock);
// order.addItem(item);
// }

// orderRepository.save(order);
// logger.info("reserveTickets SUCCESS | orderId={}", order.getId());
// return OrderMapper.toDTO(order);
// }

// public OrderDTO removeFromOrder(UUID orderId,
// UUID itemId,
// String buyerId) {
// logger.info("USE-CASE removeFromOrder | orderId={} itemId={} buyerId={}",
// orderId, itemId, buyerId);

// ActiveOrder order = findValidOrder(orderId, buyerId);

// OrderItem item = order.getItems().stream()
// .filter(i -> i.getItemId().equals(itemId))
// .findFirst()
// .orElseThrow(() -> {
// logger.warn("removeFromOrder REJECTED | item not found | itemId={}", itemId);
// return new IllegalArgumentException("Item not found: " + itemId);
// });

// order.removeItem(itemId);
// orderRepository.releaseLock(item.getLockResourceId()); // String
// orderRepository.save(order);

// logger.info("removeFromOrder SUCCESS | orderId={}", orderId);
// return OrderMapper.toDTO(order);
// }

// public PurchaseDTO checkout(UUID orderId,
// String buyerId,
// PaymentDetailsDTO paymentDTO) {
// logger.info("USE-CASE checkout | orderId={} buyerId={}", orderId, buyerId);

// ActiveOrder order = findValidOrder(orderId, buyerId);
// BigDecimal total = order.getTotal();
// PaymentDetails details = new PaymentDetails(
// paymentDTO.cardToken(), paymentDTO.billingName());

// PaymentTransaction tx = paymentService.charge(order.getId(), total, details);
// if (!tx.isSuccessful()) {
// logger.error("checkout FAILED | payment unsuccessful | orderId={}", orderId);
// throw new IllegalStateException("Payment failed for order: " + orderId);
// }

// try {
// Purchase purchase = new Purchase(order);
// List<TicketCode> codes = ticketGateway.issueTickets(
// purchase.getPurchaseId(), order.getItems());

// order.markCompleted();
// orderRepository.save(order);
// orderRepository.savePurchase(purchase);
// order.getItems().forEach(i ->
// orderRepository.releaseLock(i.getLockResourceId())); // String
// orderRepository.delete(orderId);

// List<String> ticketCodes = codes.stream()
// .map(TicketCode::getCode)
// .collect(Collectors.toList());

// logger.info("checkout SUCCESS | orderId={} purchaseId={}",
// orderId, purchase.getPurchaseId());
// return OrderMapper.toDTO(purchase, ticketCodes);

// } catch (Exception e) {
// logger.error("checkout FAILED | ticket error | orderId={} error={}",
// orderId, e.getMessage());
// paymentService.refund(tx.getTransactionId());
// logger.info("checkout ROLLBACK | refund issued | txId={}",
// tx.getTransactionId());
// throw new IllegalStateException(
// "Ticket issuance failed. Payment refunded. Order: " + orderId);
// }
// }

// public OrderDTO getActiveOrder(String buyerId, int eventId) {
// logger.info("USE-CASE getActiveOrder | buyerId={} eventId={}", buyerId,
// eventId);
// return orderRepository.findActiveOrder(buyerId, eventId)
// .map(o -> {
// logger.info("getActiveOrder SUCCESS | orderId={}", o.getId());
// return OrderMapper.toDTO(o);
// })
// .orElseThrow(() -> {
// logger.warn("getActiveOrder NOT FOUND | buyerId={} eventId={}",
// buyerId, eventId);
// return new IllegalStateException(
// "No active order: buyer=" + buyerId + " event=" + eventId);
// });
// }

// public List<PurchaseDTO> getPurchaseHistory(String buyerId) {
// logger.info("USE-CASE getPurchaseHistory | buyerId={}", buyerId);
// List<PurchaseDTO> result = orderRepository.findPurchasesByBuyer(buyerId)
// .stream()
// .map(p -> OrderMapper.toDTO(p, List.of()))
// .collect(Collectors.toList());
// logger.info("getPurchaseHistory SUCCESS | buyerId={} count={}",
// buyerId, result.size());
// return result;
// }

// private ActiveOrder findValidOrder(UUID orderId, String buyerId) {
// ActiveOrder order = orderRepository.findById(orderId)
// .orElseThrow(() -> {
// logger.warn("findValidOrder NOT FOUND | orderId={}", orderId);
// return new IllegalArgumentException("Order not found: " + orderId);
// });
// if (!order.isOwnedBy(buyerId)) {
// logger.warn("findValidOrder REJECTED | not owner | orderId={}", orderId);
// throw new IllegalStateException("Order not owned by buyer: " + buyerId);
// }
// if (order.isExpired()) {
// logger.warn("findValidOrder REJECTED | expired | orderId={}", orderId);
// throw new IllegalStateException("Order expired: " + orderId);
// }
// return order;
// }
// }