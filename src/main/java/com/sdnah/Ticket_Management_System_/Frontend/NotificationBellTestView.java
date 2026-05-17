package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Notifications.NotificationType;
import com.sdnah.Ticket_Management_System_.Frontend.NotificationBell;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("notification-bell-test")
public class NotificationBellTestView extends VerticalLayout {

    private final NotificationService notificationService;
    private final IrepresnteUserService userService;

    private NotificationBell notificationBell;
    private final VerticalLayout bellContainer = new VerticalLayout();
    private final Span status = new Span();

    public NotificationBellTestView(NotificationService notificationService,
                                    IrepresnteUserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;

        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Notification Bell Test Page");

        Paragraph explanation = new Paragraph(
                "Use this page only for development. Paste a valid login token, then test the NotificationBell component."
        );

        TextField tokenField = new TextField("Login Token");
        tokenField.setWidthFull();

        Object existingToken = VaadinSession.getCurrent().getAttribute("token");
        if (existingToken != null) {
            tokenField.setValue(existingToken.toString());
        }

        Button saveTokenButton = new Button("Save token and load bell", event -> {
            String token = tokenField.getValue();

            if (token == null || token.isBlank()) {
                Notification.show("Token is required");
                return;
            }

            try {
                String memberId = userService.requireMemberId(token.trim());

                VaadinSession.getCurrent().setAttribute("token", token.trim());

                status.setText("Logged-in memberId: " + memberId);

                loadBell();

                Notification.show("Token saved. Bell loaded.");

            } catch (Exception ex) {
                status.setText("Invalid token");
                Notification.show("Invalid token: " + ex.getMessage());
            }
        });

        Button sendTestNotificationButton = new Button("Create test notification for me", event -> {
            Object tokenObj = VaadinSession.getCurrent().getAttribute("token");

            if (tokenObj == null) {
                Notification.show("Save token first");
                return;
            }

            try {
                String memberId = userService.requireMemberId(tokenObj.toString());

                notificationService.createNotification(
                        memberId,
                        "Hello from NotificationBell test page",
                        NotificationType.ROLE_CHANGED
                );

                if (notificationBell != null) {
                    notificationBell.refreshUnreadCount();
                }

                Notification.show("Test notification created for memberId: " + memberId);

            } catch (Exception ex) {
                Notification.show("Failed to create notification: " + ex.getMessage());
            }
        });

        bellContainer.setPadding(false);
        bellContainer.setSpacing(false);

        add(
                title,
                explanation,
                tokenField,
                saveTokenButton,
                sendTestNotificationButton,
                status,
                bellContainer
        );

        if (existingToken != null) {
            try {
                String memberId = userService.requireMemberId(existingToken.toString());
                status.setText("Logged-in memberId: " + memberId);
                loadBell();
            } catch (Exception ignored) {
                status.setText("Existing token is invalid");
            }
        }
    }

    private void loadBell() {
        bellContainer.removeAll();

        notificationBell = new NotificationBell(notificationService, userService);

        bellContainer.add(notificationBell);
    }
}