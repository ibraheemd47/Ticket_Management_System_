package com.sdnah.Ticket_Management_System_.Application_Layer.Order;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.List;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.IOrderRepository;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.OrderItem;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.Lock;

public class ExpiredOrderService {
    private static final Logger logger = LoggerFactory.getLogger(ExpiredOrderService.class);
    private final IOrderRepository orderRepository;

    public ExpiredOrderService(IOrderRepository orderRepository) {
        if (orderRepository == null)
            throw new IllegalArgumentException("orderRepository required");
        this.orderRepository = orderRepository;
    }

    public void releaseExpiredOrders() {
        logger.info("Running expired orders cleanup");

        // 1. שחרר הזמנות שפגו
        List<ActiveOrder> expired = orderRepository.findExpiredOrders();
        for (ActiveOrder order : expired) {
            if (order.getStatus() == ActiveOrder.Status.ACTIVE) {
                order.markExpired();
                for (OrderItem item : order.getItems()) {
                    orderRepository.releaseLock(item.getLockResourceId());
                    item.clearLock();
                }
                orderRepository.save(order);
                logger.info("Expired order {} released", order.getId());
            }
        }

        // 2. נקה locks יתומים (ללא הזמנה פעילה)
        List<Lock> orphanedLocks = orderRepository.findExpiredLocks();
        for (Lock lock : orphanedLocks) {
            orderRepository.releaseLock(lock.getResourceId());
            logger.info("Released orphaned lock {}", lock.getResourceId());
        }

        logger.info("Expired orders cleanup done. orders={} locks={}",
                expired.size(), orphanedLocks.size());
    }
}
