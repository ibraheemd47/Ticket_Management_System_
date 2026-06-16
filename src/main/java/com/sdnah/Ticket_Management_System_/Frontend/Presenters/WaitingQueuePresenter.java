package com.sdnah.Ticket_Management_System_.Frontend.Presenters;

import org.springframework.stereotype.Component;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Waiting_QueueService;
import com.sdnah.Ticket_Management_System_.Frontend.UiPushRegistry;
import com.sdnah.Ticket_Management_System_.Frontend.WaitingQueueView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Presenter for {@link WaitingQueueView}. Owns the service interactions and
 * the session-bound state (token, user id, show id) so the view itself can
 * stay focused on layout and display methods.
 */
@Component
@UIScope
public class WaitingQueuePresenter {

    private final Waiting_QueueService queueService;
    private final UiPushRegistry pushRegistry;

    private WaitingQueueView view;
    private String token;
    private long userId;
    private long showId;

    public WaitingQueuePresenter(Waiting_QueueService queueService, UiPushRegistry pushRegistry) {
        this.queueService = queueService;
        this.pushRegistry = pushRegistry;
    }

    public void setView(WaitingQueueView view) {
        this.view = view;
    }

    /**
     * Wire the session-derived ids the presenter will use for service calls.
     * Called by the view from its {@code beforeEnter} after it has resolved
     * the session attributes.
     */
    public void bind(String token, long userId, long showId) {
        this.token = token;
        this.userId = userId;
        this.showId = showId;
    }

    public long getUserId() {
        return userId;
    }

    /** Pull current position + ETA from the service and update the view. */
    public void refresh() {
        try {
            int position = queueService.getPosition(userId, showId);
            int minutes  = queueService.calculateEstimatedWaitTimeInMinutes(userId, showId);

            if (position < 0) {
                // Either admitted or never joined — surface the "you're up" UI.
                view.showYoureUp();
            } else {
                // position is 0-indexed; show "#1" for the head of the line.
                view.showWaitingState(position + 1, minutes);
            }
        } catch (RuntimeException ex) {
            view.showQueueError(ex.getMessage());
        }
    }

    /** Register this UI with the push registry so the backend can flip its state. */
    public void registerForPush(UI ui) {
        if (token != null) {
            pushRegistry.register(token, ui);
        }
    }

    public void unregisterFromPush(UI ui) {
        if (token != null) {
            pushRegistry.unregister(token, ui);
        }
    }

    /** Dev helper — lets the user join the queue from the UI for demos. */
    public void joinQueueAsDev() {
        try {
            boolean joined = queueService.joinQueue(userId, showId);
            view.showInfo(joined ? "Joined the queue." : "Already in the queue.");
            refresh();
        } catch (RuntimeException ex) {
            view.showError("Join failed: " + ex.getMessage());
        }
    }
}
