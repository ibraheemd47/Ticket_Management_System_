package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PaymentDetailsDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.PurchaseDTO;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.DTOs.SeatRequest;
import com.sdnah.Ticket_Management_System_.Domain_Layer.OrderPolicyDomainService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Purchase;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Ticketcode;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.IPolicyRepo;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.ActiveOrderRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PurchaseRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;


@Service
@Transactional
public class ActiveOrderService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveOrderService.class);
    private static final int TTL_MINUTES = 10;

    private final ActiveOrderRepository orderRepo;
    private final PurchaseRepository purchaseRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final PaymentService paymentService;
    private final ITicketSupplierGateway ticketGateway;
    private final Ticket_Domain_Service ticketDomainService;
    private final TicketRepository ticketRepository;
    private final OrderPolicyDomainService orderPolicyDomainService;
    private final IPolicyRepo policyRepository;
    private final userOrderDomainService userOrderDomainService;

    public ActiveOrderService(
            ActiveOrderRepository orderRepo,
            PurchaseRepository purchaseRepo,
            PaymentTransactionRepository transactionRepo,
            PaymentService paymentService,
            ITicketSupplierGateway ticketGateway,
            TicketRepository ticketRepository,
            IPolicyRepo policyRepo,
            OrderPolicyDomainService orderPolicyDomainService
    ) {
        if (orderRepo == null) throw new IllegalArgumentException("orderRepo required");
        if (purchaseRepo == null) throw new IllegalArgumentException("purchaseRepo required");
        if (transactionRepo == null) throw new IllegalArgumentException("transactionRepo required");
        if (paymentService == null) throw new IllegalArgumentException("paymentService required");
        if (ticketGateway == null) throw new IllegalArgumentException("ticketGateway required");
        if (ticketRepository == null) throw new IllegalArgumentException("ticketRepository required");
        if (policyRepo == null) throw new IllegalArgumentException("policyRepo required");
        if (orderPolicyDomainService == null) throw new IllegalArgumentException("orderPolicyDomainService required");

        this.orderRepo = orderRepo;
        this.purchaseRepo = purchaseRepo;
        this.transactionRepo = transactionRepo;
        this.paymentService = paymentService;
        this.ticketGateway = ticketGateway;
        this.ticketRepository = ticketRepository;
        this.ticketDomainService = new Ticket_Domain_Service();
        this.userOrderDomainService = new userOrderDomainService();
        this.orderPolicyDomainService = orderPolicyDomainService;
        this.policyRepository = policyRepo;
    }

    public synchronized OrderDTO reserveTickets(String userToken, UUID eventId, List<SeatRequest> seats) {
        logger.info("Starting ticket reservation for userToken {} event {}", userToken, eventId);

        String buyerId = userOrderDomainService.getBuyerIdFromToken(userToken);

        if (orderRepo.findActiveOrder(buyerId, eventId).isPresent()) {
            throw new IllegalStateException("Active order already exists");
        }

        ActiveOrder order = new ActiveOrder(buyerId, eventId, TTL_MINUTES);

        try {
            for (SeatRequest seat : seats) {
                if (orderRepo.isTicketLocked(seat.getTicketId())) {
                    throw new IllegalStateException("Ticket already reserved: " + seat.getTicketId());
                }

                Lock lock = new Lock(
                        seat.getTicketId(),
                        buyerId,
                        LocalDateTime.now().plusMinutes(TTL_MINUTES)
                );

                order.addTicket(
                        seat.getTicketId(),
                        seat.getSeatId(),
                        seat.getAreaId(),
                        seat.getPrice(),
                        lock
                );
            }

            PurchasePolicy purchasePolicy = policyRepository.findPurchasePolicyByEventId(order.getEventId());
            orderPolicyDomainService.validatePurchasePolicy(order, purchasePolicy);

            DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
            orderPolicyDomainService.applyDiscountPolicy(order, policy, null);

            orderRepo.saveAndFlush(order);

            for (SeatRequest seat : seats) {
                ticketRepository.findById(UUID.fromString(seat.getTicketId()))
                        .ifPresent(t -> ticketDomainService.TicketLocked(t));
            }

            logger.info("Reservation completed successfully order {}", order.getId());
            return OrderMapper.toDTO(order);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new IllegalStateException("Ticket already reserved");
        }
    }

    public OrderDTO removeFromOrder(UUID orderId, UUID itemId, String userToken) {
        String buyerId = userOrderDomainService.getBuyerIdFromToken(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);

        order.removeTicket(itemId);

        DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
        orderPolicyDomainService.applyDiscountPolicy(order, policy, order.getAppliedCouponCode());

        orderRepo.save(order);
        return OrderMapper.toDTO(order);
    }

    public PurchaseDTO checkout(UUID orderId, String userToken, PaymentDetailsDTO paymentDTO) {
        String buyerId = userOrderDomainService.getBuyerIdFromToken(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);

        PurchasePolicy purchasePolicy = policyRepository.findPurchasePolicyByEventId(order.getEventId());
        orderPolicyDomainService.validatePurchasePolicy(order, purchasePolicy);

        PaymentDetails details = new PaymentDetails(
                paymentDTO.getCardToken(),
                paymentDTO.getBillingName(),
                paymentDTO.getPaymentMethod()
        );

        var tx = paymentService.charge(orderId, order.getFinalPrice(), details);

        if (!tx.isSuccessful()) {
            throw new IllegalStateException("Payment failed");
        }

        List<Ticketcode> codes;

        try {
            codes = ticketGateway.issueTickets(orderId, order.getItems());
        } catch (Exception e) {
            paymentService.refund(tx.getTransactionId());
            throw new IllegalStateException("Ticket issuance failed, payment refunded");
        }

        Purchase purchase = new Purchase(order);
        purchaseRepo.save(purchase);
        transactionRepo.save(tx);

        for (String lockId : order.releaseAllLocks()) {
            ticketRepository.findById(UUID.fromString(lockId))
                    .ifPresent(t -> ticketDomainService.TicketSold(order, t));
        }

        order.markCompleted();
        orderRepo.save(order);

        List<String> ticketCodes = new ArrayList<>();
        for (Ticketcode tc : codes) {
            ticketCodes.add(tc.getCode());
        }

        return OrderMapper.toDTO(purchase, ticketCodes);
    }

    public OrderDTO getActiveOrder(String userToken, UUID eventId) {
        String buyerId = userOrderDomainService.getBuyerIdFromToken(userToken);

        return orderRepo.findActiveOrder(buyerId, eventId)
                .map(OrderMapper::toDTO)
                .orElseThrow(() -> new IllegalStateException("No active order"));
    }

    public void cancelOrder(UUID orderId, String userToken) {
        String buyerId = userOrderDomainService.getBuyerIdFromToken(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);

        for (String lockId : order.releaseAllLocks()) {
            ticketRepository.findById(UUID.fromString(lockId))
                    .ifPresent(t -> ticketDomainService.TicketAvailable(t));
        }

        order.markCancelled();

        orderRepo.delete(order);
        orderRepo.flush();
    }

    public OrderDTO applyCoupon(UUID orderId, String userToken, String couponCode) {
        String buyerId = userOrderDomainService.getBuyerIdFromToken(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);

        try {
            DiscountPolicy policy = policyRepository.findDiscountPolicyByEventId(order.getEventId());
            orderPolicyDomainService.applyDiscountPolicy(order, policy, couponCode);

            orderRepo.save(order);
            return OrderMapper.toDTO(order);

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Coupon is not valid");
        }
    }

    public List<PurchaseDTO> getPurchasesByEventId(UUID eventId) {
        List<Purchase> purchases = purchaseRepo.findByEventId(eventId);
        List<PurchaseDTO> result = new ArrayList<>();

        for (Purchase p : purchases) {
            result.add(OrderMapper.toDTO(p, new ArrayList<>()));
        }

        return result;
    }

    public List<OrderDTO> getPendingOrdersByBuyer(String userToken) {
        String buyerId = userOrderDomainService.getBuyerIdFromToken(userToken);
        List<ActiveOrder> pending = orderRepo.findPendingOrdersByBuyer(buyerId);
        List<OrderDTO> result = new ArrayList<>();

        for (ActiveOrder o : pending) {
            result.add(OrderMapper.toDTO(o));
        }

        return result;
    }

    public OrderDTO getOrderById(UUID orderId, String userToken) {
        String buyerId = userOrderDomainService.getBuyerIdFromToken(userToken);
        ActiveOrder order = findValidOrder(orderId, buyerId);
        return OrderMapper.toDTO(order);
    }

    public List<PurchaseDTO> getPurchaseHistory(String userToken) {
        String buyerId = userOrderDomainService.getBuyerIdFromToken(userToken);
        List<Purchase> purchases = purchaseRepo.findByBuyerId(buyerId);
        List<PurchaseDTO> result = new ArrayList<>();

        for (Purchase p : purchases) {
            result.add(OrderMapper.toDTO(p, new ArrayList<>()));
        }

        return result;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredOrders() {
        List<ActiveOrder> expiredOrders = orderRepo.findExpiredOrders();

        for (ActiveOrder order : expiredOrders) {
            order.markExpired();

            for (String lockId : order.releaseAllLocks()) {
                ticketRepository.findById(UUID.fromString(lockId))
                        .ifPresent(t -> ticketDomainService.TicketAvailable(t));
            }

            orderRepo.save(order);
        }
    }

    private ActiveOrder findValidOrder(UUID orderId, String buyerId) {
        ActiveOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.isOwnedBy(buyerId)) {
            throw new IllegalStateException("Order does not belong to user");
        }

        if (order.isExpired()) {
            throw new IllegalStateException("Order expired");
        }

        return order;
    }
}