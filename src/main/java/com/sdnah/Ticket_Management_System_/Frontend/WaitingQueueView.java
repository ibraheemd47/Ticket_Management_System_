package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Waiting_QueueService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
//import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * "You're in line" screen. The view is opened after a user tries to reserve a
 * seat that's already locked. It shows their current position + ETA, and
 * reacts in real time (via @Push) when the queue admits them — at that point
 * a "Claim your seat" button lights up.
 *
 * <p>Real-time delivery: this view registers its server-side {@code UI} with
 * {@link UiPushRegistry} on attach. When the backend admits this user (e.g.
 * inside {@code Waiting_QueueService.admitNextUsers(...)} or in
 * {@code RealtimeNotificationSender}), it should call
 * {@code uiPushRegistry.push(token, ui -> { ... })} to flip the view to the
 * "you're up" state. No polling.
 */
// Note: @Push is enabled application-wide on TicketManagementSystemApplication,
// so this view automatically gets server-push without re-annotating here.
@Route("queue")
public class WaitingQueueView extends VerticalLayout implements BeforeEnterObserver {

    /** Vaadin session key set by LoginView. */
    private static final String SESSION_TOKEN = "token";

    /**
     * Vaadin session key the queue view needs (numeric member id) — should be
     * populated by LoginView next to the token. If you store the member id by
     * a different name, change this constant.
     */
    private static final String SESSION_MEMBER_ID = "memberId";

    /** Session key for the show the user is queueing for. */
    private static final String SESSION_SHOW_ID = "queueShowId";

    private final Waiting_QueueService queueService;
    private final UiPushRegistry pushRegistry;

    private String token;
    private long userId;
    private long showId;

    // UI handles we update after a refresh / push
    private final H1 positionLabel = new H1();
    private final Paragraph etaLabel = new Paragraph();
    private final Button claimButton = new Button("Claim your seat");
    private final Span statusBadge = new Span();
    private final Button refreshButton = new Button("Refresh");

    public WaitingQueueView(Waiting_QueueService queueService, UiPushRegistry pushRegistry) {
        this.queueService = queueService;
        this.pushRegistry = pushRegistry;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        add(buildHeader());
        add(buildCard());
    }

    // ── Auth + setup ─────────────────────────────────────────────────────────

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Object t  = UI.getCurrent().getSession().getAttribute(SESSION_TOKEN);
        if (t == null) {
            event.forwardTo(LoginView.class);
            return;
        }
        Object mi = UI.getCurrent().getSession().getAttribute(SESSION_MEMBER_ID);
        Object si = UI.getCurrent().getSession().getAttribute(SESSION_SHOW_ID);

        this.token  = t.toString();
        this.userId = mi != null ? Long.parseLong(mi.toString()) : 101L;
        this.showId = si != null ? Long.parseLong(si.toString()) : 42L;

        try {
            refreshFromBackend();
        } catch (RuntimeException ex) {
            positionLabel.setText("—");
            etaLabel.setText("No live queue data: " + ex.getMessage());
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (token != null) {
            pushRegistry.register(token, attachEvent.getUI());
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (token != null) {
            pushRegistry.unregister(token, detachEvent.getUI());
        }
        super.onDetach(detachEvent);
    }

    // ── State refresh ────────────────────────────────────────────────────────

    /** Pull current position + ETA from the service. */
    private void refreshFromBackend() {
        int position = queueService.getPosition(userId, showId);
        int minutes  = queueService.calculateEstimatedWaitTimeInMinutes(userId, showId);

        if (position < 0) {
            // Either admitted or never joined. Surface the "you're up" UI.
            showYoureUp();
            return;
        }
        // position is 0-indexed; show "#1" for the head of the line.
        positionLabel.setText("#" + (position + 1));
        etaLabel.setText("Estimated wait: " + minutes + " min");
        statusBadge.setText("Waiting");
        statusBadge.getStyle()
                .set("background", "#ffd34d")
                .set("color", "#7a5b00")
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("font-size", "12px")
                .set("font-weight", "700");
        claimButton.setVisible(false);
        refreshButton.setVisible(true);
    }

    /** Called from the @Push side when the backend admits this user. */
    public void showYoureUp() {
        positionLabel.setText("It's your turn!");
        etaLabel.setText("A seat is reserved for you. Click below before the timer runs out.");
        statusBadge.setText("Admitted");
        statusBadge.getStyle()
                .set("background", "#2e7d32")
                .set("color", "white")
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("font-size", "12px")
                .set("font-weight", "700");
        claimButton.setVisible(true);
        refreshButton.setVisible(false);
        Notification n = Notification.show("You've been admitted from the queue!", 4000,
                Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    private Div buildHeader() {
        Div header = new Div();
        header.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("padding", "28px 52px")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center");

        H2 logo = new H2("TICKET MANAGEMENT");
        logo.getStyle()
                .set("margin", "0")
                .set("font-size", "24px")
                .set("font-weight", "900");

        Div nav = new Div();
        nav.getStyle()
                .set("display", "flex")
                .set("gap", "32px")
                .set("align-items", "center");

        Span home = new Span("Home");
        home.getStyle().set("cursor", "pointer").set("font-weight", "700");
        home.addClickListener(e -> UI.getCurrent().navigate("main"));

        Span account = new Span("👤 My Account");
        account.getStyle().set("cursor", "pointer").set("font-weight", "700");
        account.addClickListener(e -> UI.getCurrent().navigate("profile"));

        nav.add(home, account);
        header.add(logo, nav);
        return header;
    }

    private Div buildCard() {
        Div card = new Div();
        card.getStyle()
                .set("max-width", "560px")
                .set("margin", "48px auto")
                .set("padding", "40px")
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0,0,0,0.06)")
                .set("text-align", "center");

        H2 title = new H2("Waiting Queue");
        title.getStyle().set("margin", "0 0 8px 0");

        Paragraph blurb = new Paragraph(
                "The seats you wanted were just taken. We'll let you know the moment one frees up.");
        blurb.getStyle().set("color", "#666").set("margin-top", "0");

        statusBadge.getStyle().set("display", "inline-block").set("margin-bottom", "16px");

        positionLabel.getStyle()
                .set("font-size", "64px")
                .set("margin", "8px 0")
                .set("color", "#026cdf");

        etaLabel.getStyle().set("color", "#444").set("margin", "0");

        claimButton.setVisible(false);
        claimButton.getStyle()
                .set("margin-top", "24px")
                .set("background", "#2e7d32")
                .set("color", "white")
                .set("font-weight", "700")
                .set("padding", "12px 28px")
                .set("border-radius", "8px");
        claimButton.addClickListener(e -> UI.getCurrent().navigate("areaselection"));

        refreshButton.getStyle()
                .set("margin-top", "24px")
                .set("background", "#026cdf")
                .set("color", "white")
                .set("font-weight", "700")
                .set("padding", "10px 24px")
                .set("border-radius", "8px");
        refreshButton.addClickListener(e -> refreshFromBackend());

        // Dev helper — lets you actually join the queue from the UI so you can
        // see real "#1, #2…" states. Remove once a real "no seats → queue me"
        // hand-off exists upstream.
        Button joinDev = new Button("(Dev) Join this queue as user " + userId, e -> {
            try {
                boolean joined = queueService.joinQueue(userId, showId);
                Notification.show(joined ? "Joined the queue." : "Already in the queue.",
                        2500, Notification.Position.TOP_CENTER);
                refreshFromBackend();
            } catch (RuntimeException ex) {
                Notification.show("Join failed: " + ex.getMessage(), 3500,
                                Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        joinDev.getStyle()
                .set("margin-top", "12px")
                .set("background", "#fff")
                .set("color", "#666")
                .set("border", "1px dashed #cdd9ec")
                .set("font-size", "12px")
                .set("padding", "6px 16px")
                .set("border-radius", "999px");

        card.add(title, blurb, statusBadge, positionLabel, etaLabel,
                claimButton, refreshButton, joinDev);

        Div outer = new Div(card);
        outer.setWidthFull();
        outer.getStyle().set("padding-top", "16px");
        return outer;
    }
}
