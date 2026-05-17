package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.NotificationDTO;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.shared.Registration;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationBell extends Div {

    private final NotificationService notificationService;
    private final IrepresnteUserService userService;

    private final Button bellButton = new Button();
    private final Span badge = new Span();

    private final Dialog dialog = new Dialog();
    private final VerticalLayout notificationList = new VerticalLayout();
    private Registration pollRegistration;
    private String currentMemberId;

    public NotificationBell(NotificationService notificationService,
            IrepresnteUserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;

        buildBellButton();
        buildDialog();

        add(bellButton);
    }

    // When the page loads, the bell updates itself.
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        if (!resolveCurrentUser()) {
            setVisible(false);
            return;
        }

        setVisible(true);
        refreshUnreadCount();
        UI ui = attachEvent.getUI();

        // Poll every 3 seconds to update the badge automatically
        ui.setPollInterval(3000);

        pollRegistration = ui.addPollListener(event -> {
            if (!resolveCurrentUser()) {
                setVisible(false);
                return;
            }

            refreshUnreadCount();

            if (dialog.isOpened()) {
                refreshNotificationList();
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }

        super.onDetach(detachEvent);
    }

    private void buildBellButton() {
        bellButton.setText("🔔");
        bellButton.getStyle()
                .set("position", "relative")
                .set("border-radius", "50%")
                .set("width", "44px")
                .set("height", "44px")
                .set("font-size", "20px")
                .set("background", "white")
                .set("color", "#026cdf")
                .set("border", "1px solid #d0d7e2")
                .set("cursor", "pointer");

        badge.getStyle()
                .set("position", "absolute")
                .set("top", "-6px")
                .set("right", "-6px")
                .set("background", "#e53935")
                .set("color", "white")
                .set("border-radius", "50%")
                .set("min-width", "20px")
                .set("height", "20px")
                .set("font-size", "12px")
                .set("font-weight", "700")
                .set("display", "none")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("padding", "0 5px");

        bellButton.getElement().appendChild(badge.getElement());

        bellButton.addClickListener(event -> {
            if (!resolveCurrentUser()) {
                Notification.show("Please login first.");
                return;
            }

            refreshUnreadCount();
            refreshNotificationList();
            dialog.open();
        });
    }

    private void buildDialog() {
        dialog.setHeaderTitle("Notifications");
        dialog.setWidth("430px");
        dialog.setMaxHeight("600px");

        Button refreshButton = new Button("Refresh", event -> {
            refreshNotificationList();
            refreshUnreadCount();
        });

        Button markAllButton = new Button("Mark all as read", event -> {
            if (!resolveCurrentUser()) {
                return;
            }

            notificationService.markAllAsRead(currentMemberId);
            refreshNotificationList();
            refreshUnreadCount();
        });

        Div actions = new Div();
        actions.add(refreshButton, markAllButton);
        actions.getStyle()
                .set("display", "flex")
                .set("gap", "10px")
                .set("margin-bottom", "12px");

        notificationList.setPadding(false);
        notificationList.setSpacing(true);
        notificationList.setWidthFull();

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.add(actions, notificationList);

        dialog.add(content);
    }

    private boolean resolveCurrentUser() {
        Object tokenObj = VaadinSession.getCurrent().getAttribute("token");

        if (tokenObj == null) {
            return false;
        }

        String token = tokenObj.toString();

        if (token.isBlank()) {
            return false;
        }

        try {
            currentMemberId = userService.requireMemberId(token);
            return currentMemberId != null && !currentMemberId.isBlank();
        } catch (Exception e) {
            currentMemberId = null;
            return false;
        }
    }

    public void refreshUnreadCount() {
        if (currentMemberId == null || currentMemberId.isBlank()) {
            return;
        }

        long unreadCount = notificationService.getUnreadCount(currentMemberId);

        if (unreadCount <= 0) {
            badge.setText("");
            badge.getStyle().set("display", "none");
        } else {
            badge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            badge.getStyle().set("display", "inline-flex");
        }
    }

    public void refreshNotificationList() {
        notificationList.removeAll();

        if (currentMemberId == null || currentMemberId.isBlank()) {
            notificationList.add(emptyState("Please login to see notifications."));
            return;
        }

        List<NotificationDTO> unreadNotifications = notificationService.getUnreadNotificationsForUser(currentMemberId);

        if (unreadNotifications.isEmpty()) {
            notificationList.add(emptyState("No unread notifications."));
            return;
        }

        for (NotificationDTO notification : unreadNotifications) {
            notificationList.add(createNotificationCard(notification));
        }
    }

    private Div createNotificationCard(NotificationDTO notification) {
        Div card = new Div();
        card.getStyle()
                .set("padding", "14px")
                .set("border", "1px solid #e0e6ef")
                .set("border-radius", "10px")
                .set("background", "#ffffff")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.05)")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        H3 title = new H3(getNotificationTitle(notification));
        title.getStyle()
                .set("font-size", "15px")
                .set("margin", "0 0 6px 0")
                .set("color", "#111827");

        Div message = new Div();
        message.setText(notification.getMessage());
        message.getStyle()
                .set("font-size", "14px")
                .set("color", "#374151")
                .set("line-height", "1.5")
                .set("margin-bottom", "10px");

        Span meta = new Span(getNotificationMeta(notification));
        meta.getStyle()
                .set("font-size", "12px")
                .set("color", "#6b7280");

        Button markReadButton = new Button("Mark as read", event -> {
            notificationService.markAsRead(notification.getId(), currentMemberId);
            refreshNotificationList();
            refreshUnreadCount();
        });

        markReadButton.getStyle()
                .set("margin-top", "10px")
                .set("background", "#026cdf")
                .set("color", "white")
                .set("border-radius", "7px")
                .set("cursor", "pointer");

        card.add(title, message, meta, markReadButton);
        return card;
    }

    private Div emptyState(String text) {
        Div empty = new Div();
        empty.setText(text);
        empty.getStyle()
                .set("padding", "20px")
                .set("text-align", "center")
                .set("color", "#6b7280")
                .set("background", "#f9fafb")
                .set("border-radius", "10px")
                .set("border", "1px dashed #d1d5db");
        return empty;
    }

    private String getNotificationTitle(NotificationDTO notification) {
        if (notification.getType() == null) {
            return "Notification";
        }

        return notification.getType().toString()
                .replace("_", " ")
                .toLowerCase();
    }

    private String getNotificationMeta(NotificationDTO notification) {
        if (notification.getCreatedAt() == null) {
            return "";
        }

        try {
            return notification.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            return notification.getCreatedAt().toString();
        }
    }

    /**
     * Call this later from live notification integration.
     * For now, it is useful if another component wants to refresh the bell.
     */
    public void onNewNotification(NotificationDTO notification) {
        UI ui = UI.getCurrent();

        if (ui == null) {
            refreshUnreadCount();
            return;
        }

        ui.access(() -> {
            refreshUnreadCount();

            Notification.show(
                    notification.getMessage(),
                    4000,
                    Notification.Position.TOP_END);

            if (dialog.isOpened()) {
                refreshNotificationList();
            }
        });
    }
}