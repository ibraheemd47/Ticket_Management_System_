package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ActiveOrderService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.OrderDTOs.OrderDTO;
import com.sdnah.Ticket_Management_System_.Frontend.ManagerOrderDetails;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Presenter for {@link ManagerOrderDetails}. Resolves the order id from the
 * session, loads the OrderDTO via {@link ActiveOrderService}, and handles
 * the cancel-order action — the view only renders what it's told to.
 */
@Component
@UIScope
public class ManagerOrderDetailsPresenter {

    private final ActiveOrderService orderService;

    private ManagerOrderDetails view;
    private String token;
    private UUID orderId;

    public ManagerOrderDetailsPresenter(ActiveOrderService orderService) {
        this.orderService = orderService;
    }

    public void setView(ManagerOrderDetails view) {
        this.view = view;
    }

    public UUID getOrderId() {
        return orderId;
    }

    /**
     * Wire session data, attempt to parse the order id, and load the order.
     * Calls back into the view with one of: {@code showOrder}, {@code showPicker},
     * {@code showPickerWithError}.
     */
    public void loadFromSession(String token, Object rawOrderId) {
        this.token = token;

        if (rawOrderId == null) {
            view.showPicker();
            return;
        }
        try {
            this.orderId = UUID.fromString(rawOrderId.toString());
            OrderDTO order = orderService.getOrderById(orderId, token);
            view.showOrder(order);
        } catch (IllegalArgumentException badUuid) {
            view.showPickerWithError("Invalid order id: " + rawOrderId);
        } catch (RuntimeException ex) {
            view.showPickerWithError("Couldn't load order: " + ex.getMessage());
        }
    }

    /** Cancel the order this presenter is currently bound to. */
    public void cancelOrder() {
        try {
            orderService.cancelOrder(orderId, token);
            view.onCancelSucceeded();
        } catch (RuntimeException ex) {
            view.onCancelFailed(ex.getMessage());
        }
    }

    /**
     * Remove a single ticket/item from the active order (II.2.7 — manage active
     * order: remove tickets / reduce quantity). Backend enforces order ownership
     * and reapplies the discount policy to the reduced order.
     */
    public void removeItem(UUID itemId) {
        try {
            orderService.removeFromOrder(orderId, itemId, token);
            view.onItemRemoved();
        } catch (RuntimeException ex) {
            view.onRemoveFailed(ex.getMessage());
        }
    }

    /**
     * Undo the most recently logged action on this order (II.2.7 — undo
     * last action). Currently the backend supports reversing a REMOVE_TICKET;
     * other action types throw a friendly message that the view shows.
     */
    public void undoLastAction() {
        try {
            orderService.undoLast(orderId, token);
            view.onUndoSucceeded();
        } catch (RuntimeException ex) {
            view.onUndoFailed(ex.getMessage());
        }
    }
}
