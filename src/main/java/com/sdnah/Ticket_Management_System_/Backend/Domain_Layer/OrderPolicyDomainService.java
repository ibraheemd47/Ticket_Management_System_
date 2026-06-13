package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.LotteryEntry;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.SellingPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount.DiscountPolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchaseContext;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.PurchasePolicy;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Purchase.RuleResult;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.IEventRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.LotteryRepository;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PolicyRepository;
@Component
public class OrderPolicyDomainService {

    private final PolicyRepository policyRepository;
    private final LotteryRepository lotteryRepository;
    private final IEventRepository eventRepository;

    public OrderPolicyDomainService(PolicyRepository policyRepository,
                                    LotteryRepository lotteryRepository,
                                    IEventRepository eventRepository) {
        if (policyRepository == null)
            throw new IllegalArgumentException("policyRepository required");
        this.policyRepository = policyRepository;
        this.lotteryRepository = lotteryRepository;
        this.eventRepository = eventRepository;
    }

    // =========================================================================
    // UC II.2.4 + II.2.8 — Validate purchase + apply discounts
    // =========================================================================
    public void validateAndApplyDiscounts(ActiveOrder order, String couponCode,
                                          int buyerAge, boolean isMember) {
        validateOrder(order);
        // Purchase: validate BOTH event AND company policies (AND logic)
        validatePurchasePolicy(order, findEventPurchasePolicy(order), buyerAge, isMember);
        validatePurchasePolicy(order, findCompanyPurchasePolicy(order), buyerAge, isMember);
        // Discount: event-first, company-fallback
        applyDiscountPolicy(order, findDiscountPolicy(order), couponCode);
    }

    // =========================================================================
    // UC II.2.8 — Apply discounts only (used in applyCoupon / removeFromOrder)
    // =========================================================================
    public void applyDiscounts(ActiveOrder order, String couponCode) {
        validateOrder(order);
        applyDiscountPolicy(order, findDiscountPolicy(order), couponCode);
    }

    // =========================================================================
    // UC II.2.4 — Validate purchase policy only
    // =========================================================================
    public void validatePurchasePolicy(ActiveOrder order, int buyerAge, boolean isMember) {
        validateOrder(order);
        // validate both event AND company purchase policies
        validatePurchasePolicy(order, findEventPurchasePolicy(order), buyerAge, isMember);
        validatePurchasePolicy(order, findCompanyPurchasePolicy(order), buyerAge, isMember);
    }

    // =========================================================================
    // Private: find purchase policies
    // =========================================================================

    // event purchase policy — null if none defined
    private PurchasePolicy findEventPurchasePolicy(ActiveOrder order) {
        Object result = policyRepository.findPurchasePolicyByEventId(order.getEventId());
        return toPolicy(result, PurchasePolicy.class);
    }

    // company purchase policy — null if none defined
    private PurchasePolicy findCompanyPurchasePolicy(ActiveOrder order) {
        UUID companyId = getCompanyIdFromEvent(order);
        if (companyId == null) return null;
        return policyRepository
                .findPurchasePolicyByCompanyIdAndEventIdIsNull(companyId)
                .orElse(null);
    }
   

    // discount: event-first, company-fallback
    private DiscountPolicy findDiscountPolicy(ActiveOrder order) {
        Object result = policyRepository.findDiscountPolicyByEventId(order.getEventId());
        DiscountPolicy eventPolicy = toPolicy(result, DiscountPolicy.class);

        System.out.println("DEBUG eventPolicy=" + eventPolicy);
        if (eventPolicy != null) System.out.println("DEBUG rootRule=" + eventPolicy.getRootRule());

        if (eventPolicy != null && eventPolicy.getRootRule() != null) return eventPolicy;

        UUID companyId = getCompanyIdFromEvent(order);
        System.out.println("DEBUG companyId=" + companyId);

        if (companyId == null) return null;
        DiscountPolicy companyPolicy = policyRepository
                .findDiscountPolicyByCompanyIdAndEventIdIsNull(companyId)
                .orElse(null);
        System.out.println("DEBUG companyPolicy=" + companyPolicy);
        if (companyPolicy != null) System.out.println("DEBUG companyRootRule=" + companyPolicy.getRootRule());
        return companyPolicy;
    }

    // =========================================================================
    // UC II.2.8 — Apply discount policy to order
    // =========================================================================
    public void applyDiscountPolicy(ActiveOrder order, DiscountPolicy policy, String couponCode) {
        validateOrder(order);
        double originalTotal = order.getTotal().doubleValue();
        int    quantity      = order.getItems().size();

        if (policy == null) {
            order.updateFinalPrice(originalTotal);
            return;
        }

        DiscountContext context = new DiscountContext(
                quantity,
                LocalDateTime.now(),
                couponCode,
                originalTotal,
                order.getEventId()
        );

        double finalPrice = policy.computeFinalPrice(originalTotal, context);
        order.updateFinalPrice(finalPrice);

        if (couponCode != null && !couponCode.isBlank()) {
            order.setAppliedCouponCode(couponCode);
        }
    }

    // =========================================================================
    // UC II.2.4 — Validate a single purchase policy against order
    // null = no restrictions → allowed
    // =========================================================================
    private void validatePurchasePolicy(ActiveOrder order, PurchasePolicy policy,
                                        int buyerAge, boolean isMember) {
        if (policy == null) return;

        int quantity = order.getItems().size();
        PurchaseContext context = new PurchaseContext(
                buyerAge,
                quantity,
                false,
                isMember,
                LocalDateTime.now()
        );
        RuleResult result = policy.validate(context);
        if (!result.isAllowed()) {
            throw new IllegalStateException(result.getMessage());
        }
    }

    // =========================================================================
    // Selling policy validation (for lottery access code)
    // =========================================================================
    public void validateSellingPolicy(ActiveOrder order, String memberId) {
        validateSellingPolicy(order, memberId, null);
    }

    public void validateSellingPolicy(ActiveOrder order, String memberId, String enteredCode) {
       

        LocalDateTime now = LocalDateTime.now();

        Lottery lottery = lotteryRepository.findByEventId(order.getEventId()).stream()
            .filter(l -> l.getStatus() == Lottery.LotteryStatus.DRAWN)
            .max(java.util.Comparator.comparing(Lottery::getDrawTime))
            .orElseThrow(() -> new IllegalStateException("Lottery has not been drawn yet"));

    
        if (lottery.getOpenSaleTime() != null
            && !now.isBefore(lottery.getOpenSaleTime())) {
            return; // public sale is open — code is no longer needed
        }

        String code = enteredCode == null ? "" : enteredCode.trim();

        lotteryRepository.findByEventId(order.getEventId()).stream()
                .flatMap(l -> l.getEntries().stream())
                .filter(e -> e.getMemberId().equals(memberId))
                .filter(LotteryEntry::isAccessCodeValid)
                .filter(e -> e.getAccessCode().equalsIgnoreCase(code))   // ← הקוד סוף סוף עושה עבודה
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Invalid or missing lottery access code for this event"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

 
     private UUID getCompanyIdFromEvent(ActiveOrder order) {
        // Try SellingPolicy first
        Object result = policyRepository.findSellingPolicyByEventId(order.getEventId());
        SellingPolicy sp = toSellingPolicy(result);
        if (sp != null) return sp.getCompanyId();

        // Try PurchasePolicy
        Object pp = policyRepository.findPurchasePolicyByEventId(order.getEventId());
        PurchasePolicy purchasePolicy = toPolicy(pp, PurchasePolicy.class);
        if (purchasePolicy != null) return purchasePolicy.getCompanyId();

        // Try DiscountPolicy
        Object dp = policyRepository.findDiscountPolicyByEventId(order.getEventId());
        DiscountPolicy discountPolicy = toPolicy(dp, DiscountPolicy.class);
        if (discountPolicy != null) return discountPolicy.getCompanyId();

        // Fallback — get companyId directly from Event entity
        return eventRepository.findById(order.getEventId())
                .map(e -> e.getCompanyId())
                .orElse(null);
    }

    private SellingPolicy toSellingPolicy(Object result) {
        if (result == null) return null;
        if (result instanceof Optional) return ((Optional<SellingPolicy>) result).orElse(null);
        return (SellingPolicy) result;
    }

    @SuppressWarnings("unchecked")
    private <T> T toPolicy(Object result, Class<T> type) {
        if (result == null) return null;
        if (result instanceof Optional) return ((Optional<T>) result).orElse(null);
        return type.isInstance(result) ? type.cast(result) : null;
    }

    // =========================================================================
    // Shared order validation
    // =========================================================================
    private void validateOrder(ActiveOrder order) {
        if (order == null)
            throw new IllegalArgumentException("Order is required");
        if (order.getStatus() != ActiveOrder.Status.ACTIVE)
            throw new IllegalStateException("Order is not active");
        if (order.isExpired())
            throw new IllegalStateException("Order has expired");
        if (order.getItems().isEmpty())
            throw new IllegalStateException("Order is empty");
    }
}