package com.sdnah.Ticket_Management_System_.Frontend;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.sdnah.Ticket_Management_System_.Frontend.Presenters.MainPresenter;
import com.sdnah.Ticket_Management_System_.Frontend.Presenters.NotificationBellPresenter;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.CompanyDTO;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route("main")
public class MainView extends VerticalLayout {

    private final MainPresenter presenter;
    private final NotificationBellPresenter notificationBell;
    private Grid<EventDto> eventGrid;
    private HorizontalLayout companyList;

    public MainView(MainPresenter presenter, NotificationBellPresenter notificationBell) {
        this.presenter = presenter;
        this.notificationBell = notificationBell;

        // 1. Link this view directly to the Presenter
        this.presenter.setView(this);
        this.notificationBell.setView(this);

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        setupHeader();
        setupQuickAccessBar(); 
        setupEventSection();
        setupCompanySection();
        setupHottestArtistsSection(); 
        setupFooter(); 

        // 2. Ask presenter to load initial data into the view
        this.presenter.loadInitialData();
    }

    private void setupQuickAccessBar() {
        if (UI.getCurrent().getSession().getAttribute("token") == null) {
            return; 
        }

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.getStyle()
                .set("background", "#e8f1ff")
                .set("padding", "12px 52px")
                .set("box-sizing", "border-box")
                .set("border-bottom", "1px solid #cdd9ec");

        bar.add(
                quickLink("⏳  Waiting queue", "queue"),
                quickLink("📋  Manage an order", "manager/order"),
                quickLink("🏢  Manage company", "company"),
                quickLink("Create New Company", "company-create"));
        add(bar);
    }

    private Button quickLink(String label, String route) {
        Button b = new Button(label, e -> UI.getCurrent().navigate(route));
        b.getStyle()
                .set("background", "white")
                .set("color", "#026cdf")
                .set("border", "1px solid #cdd9ec")
                .set("border-radius", "999px")
                .set("padding", "6px 16px")
                .set("font-weight", "700")
                .set("cursor", "pointer");
        return b;
    }

    private void setupHeader() {
        HorizontalLayout header = new HorizontalLayout();

        header.getStyle()
                .set("background", "#026cdf")
                .set("padding", "20px 52px")
                .set("width", "100%")
                .set("box-sizing", "border-box");
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H1 logo = new H1("TICKET MANAGEMENT");
        logo.getStyle()
                .set("color", "black") 
                .set("margin", "0")
                .set("font-size", "24px")
                .set("font-weight", "900")
                .set("cursor", "pointer");
        logo.addClickListener(e -> UI.getCurrent().navigate(MainView.class));

        VerticalLayout searchBarLayout = setupSearchBar();
        searchBarLayout.getStyle().set("margin", "0 40px");

        HorizontalLayout authButtons = new HorizontalLayout();

        Object token = UI.getCurrent().getSession().getAttribute("token");

        if (token == null) {
            Button loginBtn = new Button("Login", e -> UI.getCurrent().navigate("login"));
            loginBtn.getStyle().set("background", "white").set("color", "#026cdf")
                    .set("font-weight", "700").set("border-radius", "8px").set("cursor", "pointer");

            Button signupBtn = new Button("Sign Up", e -> UI.getCurrent().navigate("signup"));
            signupBtn.getStyle().set("background", "transparent").set("color", "white")
                    .set("border", "2px solid white").set("font-weight", "700")
                    .set("border-radius", "8px").set("cursor", "pointer");

            authButtons.add(loginBtn, signupBtn);
        } else {
            Button profileBtn = new Button("My Profile", e -> UI.getCurrent().navigate("profile"));
            profileBtn.getStyle().set("background", "white").set("color", "#026cdf")
                    .set("font-weight", "700").set("border-radius", "8px").set("cursor", "pointer");

            String currentToken = (String) token;
            boolean isGuest = currentToken.startsWith("GUEST_"); // ← GUEST SUPPORT

            if (!isGuest) {
                // ── Member-only header buttons ──────────────────────────────

                // Admin button - Delegated to Presenter
                if (presenter.isSystemAdmin(currentToken)) {
                    Button adminBtn = new Button("Admin Dashboard",
                            e -> UI.getCurrent().navigate("admin"));
                    adminBtn.getStyle()
                            .set("background", "#111827").set("color", "white")
                            .set("font-weight", "700").set("border-radius", "8px")
                            .set("cursor", "pointer");
                    authButtons.add(adminBtn);
                }

                Button complaintBtn = new Button("File Complaint",
                        e -> UI.getCurrent().navigate("complaints"));
                complaintBtn.getStyle()
                        .set("background", "white").set("color", "#026cdf")
                        .set("font-weight", "700").set("border-radius", "8px")
                        .set("cursor", "pointer");
                authButtons.add(complaintBtn);

                Button logoutBtn = new Button("Logout", e -> {
                    presenter.logout(currentToken);
                });
                logoutBtn.getStyle().set("background", "transparent").set("color", "white")
                        .set("border", "2px solid white").set("font-weight", "700")
                        .set("border-radius", "8px").set("cursor", "pointer");

                NotificationBell notifcation_bell = notificationBell.CreateNotificationBell();
                authButtons.add(notifcation_bell, profileBtn, logoutBtn);

            } else {
                // ── Guest: show Login + Sign Up buttons ─────────────────────
                Button loginBtn = new Button("Login",
                        e -> UI.getCurrent().navigate("login"));
                loginBtn.getStyle().set("background", "white").set("color", "#026cdf")
                        .set("font-weight", "700").set("border-radius", "8px")
                        .set("cursor", "pointer");

                Button signupBtn = new Button("Sign Up",
                        e -> UI.getCurrent().navigate("signup"));
                signupBtn.getStyle().set("background", "transparent").set("color", "white")
                        .set("border", "2px solid white").set("font-weight", "700")
                        .set("border-radius", "8px").set("cursor", "pointer");

                authButtons.add(loginBtn, signupBtn);
            }
        }

        header.add(logo, searchBarLayout, authButtons);
        header.expand(searchBarLayout);
        add(header);
    }

    private void setupEventSection() {
        Div container = createSectionContainer();
        Div headerContainer = createBlueHeaderContainer("Upcoming Events");

        eventGrid = new Grid<>(EventDto.class, false);
        eventGrid.addColumn(ev -> ev.name).setHeader("Event Name").setFlexGrow(2);
        eventGrid.addColumn(ev -> ev.venue != null ? ev.venue : "—")
                .setHeader("Venue").setFlexGrow(2);
        eventGrid.addColumn(ev -> ev.eventType != null ? ev.eventType.name() : "—")
                .setHeader("Type").setFlexGrow(1);
        eventGrid.addComponentColumn(ev -> buildRatingStars(presenter.getEventAverageRating(ev.id)))
                .setHeader("Rating").setFlexGrow(1);
        eventGrid.addComponentColumn(ev -> {
            Button btn = new Button("View Event", e -> {
                UI.getCurrent().getSession().setAttribute("eventId", ev.id.toString());
                UI.getCurrent().navigate("EventDetails");
            });
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn.getStyle().set("font-size", "13px").set("cursor", "pointer");
            return btn;
        }).setHeader("").setAutoWidth(true);

        eventGrid.setAllRowsVisible(true);
        eventGrid.getStyle().set("border-radius", "0 0 8px 8px");
        container.add(headerContainer, eventGrid);
        add(container);
    }

    private void setupCompanySection() {
        Div container = createSectionContainer();
        Div headerContainer = createBlueHeaderContainer("Our Partner Companies");

        companyList = new HorizontalLayout();
        companyList.setWidthFull();
        companyList.setSpacing(true);
        companyList.getStyle()
                .set("background", "white")
                .set("padding", "20px")
                .set("border-radius", "0 0 8px 8px");

        container.add(headerContainer, companyList);
        add(container);
    }

    private void setupHottestArtistsSection() {
        Div container = createSectionContainer();
        Div headerContainer = createBlueHeaderContainer("Hottest Artists");

        HorizontalLayout artistList = new HorizontalLayout();
        artistList.setWidthFull();
        artistList.setJustifyContentMode(JustifyContentMode.AROUND);
        artistList.getStyle()
                .set("background", "white")
                .set("padding", "40px 20px")
                .set("border-radius", "0 0 8px 8px");

        artistList.add(
                createArtistCard("Drake",
                        "https://ui-avatars.com/api/?name=Drake&background=0D8ABC&color=fff&size=100"),
                createArtistCard("J. Cole",
                        "https://ui-avatars.com/api/?name=J+Cole&background=111827&color=fff&size=100"),
                createArtistCard("Kendrick Lamar",
                        "https://ui-avatars.com/api/?name=Kendrick+Lamar&background=E53E3E&color=fff&size=100"),
                createArtistCard("The Weeknd",
                        "https://ui-avatars.com/api/?name=The+Weeknd&background=D69E2E&color=fff&size=100"));

        container.add(headerContainer, artistList);
        add(container);
    }

    private Div createArtistCard(String name, String imageUrl) {
        Div card = new Div();
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("cursor", "pointer") 
                .set("transition", "transform 0.2s");

        card.getElement().addEventListener("mouseover", e -> card.getStyle().set("transform", "scale(1.05)"));
        card.getElement().addEventListener("mouseout", e -> card.getStyle().set("transform", "scale(1)"));

        Image img = new Image(imageUrl, name);
        img.setWidth("100px");
        img.setHeight("100px");
        img.getStyle()
                .set("border-radius", "50%") 
                .set("object-fit", "cover")
                .set("box-shadow", "0 4px 10px rgba(0,0,0,0.1)");

        H3 artistName = new H3(name);
        artistName.getStyle().set("margin-top", "15px").set("color", "#111827");

        card.add(img, artistName);

        card.addClickListener(e -> {
            System.out.println("Navigating to shows for: " + name);
        });

        return card;
    }

    private void setupFooter() {
        Div footer = new Div();
        footer.setWidthFull();
        footer.getStyle()
                .set("background", "#111827") 
                .set("color", "white")
                .set("padding", "50px 52px")
                .set("margin-top", "60px") 
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("box-sizing", "border-box");

        Div aboutSection = new Div();
        aboutSection.setWidth("50%");
        H2 aboutTitle = new H2("About VibePass");
        aboutTitle.getStyle().set("margin-top", "0").set("color", "white");

        Paragraph aboutDesc = new Paragraph(
                "VibePass is your ultimate gateway to live entertainment. We connect fans directly with the artists, teams, and events they love. Our mission is to provide a seamless, fair, and incredibly secure ticketing experience so you can focus on enjoying the show.");
        aboutDesc.getStyle().set("line-height", "1.6").set("color", "#9ca3af");
        aboutSection.add(aboutTitle, aboutDesc);

        Div socialSection = new Div();
        H2 socialTitle = new H2("Follow Us");
        socialTitle.getStyle().set("margin-top", "0").set("color", "white");

        HorizontalLayout iconsLayout = new HorizontalLayout();
        iconsLayout.setSpacing(true);

        iconsLayout.add(
                createSocialIcon(VaadinIcon.MOBILE),
                createSocialIcon(VaadinIcon.TWITTER),
                createSocialIcon(VaadinIcon.FACEBOOK),
                createSocialIcon(VaadinIcon.YOUTUBE));

        socialSection.add(socialTitle, iconsLayout);
        footer.add(aboutSection, socialSection);

        add(footer);
    }

    private Icon createSocialIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.getStyle()
                .set("color", "white")
                .set("cursor", "pointer")
                .set("width", "30px")
                .set("height", "30px");

        icon.addClickListener(e -> System.out.println("Social icon clicked!"));
        return icon;
    }

    private VerticalLayout setupSearchBar() {
        VerticalLayout searchWrapper = new VerticalLayout();
        searchWrapper.setPadding(false);
        searchWrapper.setSpacing(false);
        searchWrapper.setAlignItems(Alignment.CENTER);
        searchWrapper.getStyle().set("max-width", "850px").set("flex-grow", "1");

        HorizontalLayout pills = new HorizontalLayout();
        pills.setSpacing(true);
        pills.getStyle().set("margin-bottom", "10px");

        Button eventPill = createPill("Event", VaadinIcon.TICKET);
        Button companyPill = createPill("Company", VaadinIcon.OFFICE);
        Button venuePill = createPill("Venue", VaadinIcon.MAP_MARKER);

        pills.add(eventPill, companyPill, venuePill);

        HorizontalLayout searchBox = new HorizontalLayout();
        searchBox.setWidthFull();
        searchBox.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        searchBox.getStyle()
                .set("background", "white")
                .set("border-radius", "50px")
                .set("padding", "6px 8px 6px 24px")
                .set("box-shadow", "0 4px 15px rgba(0,0,0,0.15)");

        TextField searchInput = new TextField();
        searchInput.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchInput.setWidthFull();
        searchInput.setValueChangeMode(ValueChangeMode.EAGER);
        searchInput.getStyle()
                .set("--vaadin-input-field-border-width", "0")
                .set("background", "transparent");

        Div divider = new Div();
        divider.getStyle()
                .set("width", "1px")
                .set("height", "30px")
                .set("background", "#e5e7eb")
                .set("margin", "0 10px");

        DatePicker startDate = new DatePicker();
        startDate.setPlaceholder("Start date");
        startDate.setWidth("140px");
        startDate.getStyle().set("--vaadin-input-field-border-width", "0");

        DatePicker endDate = new DatePicker();
        endDate.setPlaceholder("End date");
        endDate.setWidth("140px");
        endDate.getStyle().set("--vaadin-input-field-border-width", "0");

        AtomicReference<String> activeFilter = new AtomicReference<>("Event");

        searchInput.addValueChangeListener(e -> {
            String value = e.getValue() != null ? e.getValue().trim() : "";
            if (value.isEmpty() && startDate.getValue() == null && endDate.getValue() == null) {
                presenter.performSearch(activeFilter.get(), "", null, null);
            }
        });

        Button searchBtn = new Button("Search");
        searchBtn.getStyle()
                .set("background", "#026cdf")
                .set("color", "white")
                .set("border-radius", "50px")
                .set("padding", "0 28px")
                .set("height", "44px")
                .set("font-weight", "bold")
                .set("cursor", "pointer");

        searchBox.add(searchInput, divider, startDate, endDate, searchBtn);

        Runnable updateUI = () -> {
            eventPill.getStyle().set("background", "transparent").set("color", "white");
            companyPill.getStyle().set("background", "transparent").set("color", "white");
            venuePill.getStyle().set("background", "transparent").set("color", "white");

            String current = activeFilter.get();

            if (current.equals("Event")) {
                eventPill.getStyle().set("background", "white").set("color", "#026cdf").set("border-radius", "20px");
                searchInput.setPlaceholder("Search concerts, shows, sports...");
                divider.setVisible(true);
                startDate.setVisible(true);
                endDate.setVisible(true);
            } else if (current.equals("Company")) {
                companyPill.getStyle().set("background", "white").set("color", "#026cdf").set("border-radius", "20px");
                searchInput.setPlaceholder("Search promoters, organizers...");
                divider.setVisible(true);
                startDate.setVisible(true);
                endDate.setVisible(true);
            } else if (current.equals("Venue")) {
                venuePill.getStyle().set("background", "white").set("color", "#026cdf").set("border-radius", "20px");
                searchInput.setPlaceholder("Search arenas, theaters, clubs...");
                divider.setVisible(false);
                startDate.setVisible(false);
                endDate.setVisible(false);
            }
        };

        eventPill.addClickListener(e -> {
            activeFilter.set("Event");
            updateUI.run();
        });

        companyPill.addClickListener(e -> {
            activeFilter.set("Company");
            updateUI.run();
        });

        venuePill.addClickListener(e -> {
            activeFilter.set("Venue");
            updateUI.run();
        });

        updateUI.run();

        searchBtn.addClickListener(e -> {
            Date start = startDate.getValue() != null
                    ? Date.from(startDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant())
                    : null;

            Date end = endDate.getValue() != null
                    ? Date.from(endDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant())
                    : null;

            // Delegated to presenter
            presenter.performSearch(activeFilter.get(), searchInput.getValue(), start, end);
        });

        searchWrapper.add(pills, searchBox);
        return searchWrapper;
    }

    private Button createPill(String text, VaadinIcon icon) {
        Button btn = new Button(text, icon.create());
        btn.getStyle()
                .set("background", "transparent")
                .set("color", "white")
                .set("border", "none")
                .set("font-weight", "600")
                .set("padding", "6px 16px")
                .set("cursor", "pointer");
        return btn;
    }

    // --- REPLACES 'performModernSearch' - THESE ARE CALLED BY THE PRESENTER ---

    public void showEvents(List<EventDto> events) {
        eventGrid.setItems(events);
    }

    public void clearEvents() {
        eventGrid.setItems();
    }

    public void showNotification(String message, boolean isError) {
        Notification notification = Notification.show(message, 4000, Notification.Position.MIDDLE);
        if (isError) {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
        }
    }

    /**
     * Logout handler. Clears every session attribute the previous user left
     * behind — not just {@code token} — so per-flow state (managingCompanyId,
     * eventId, managerOrderId, queueShowId, checkout*, etc.) doesn't leak
     * across users sharing the same browser session.
     *
     * <p>We iterate the wrapped HttpSession instead of calling {@code
     * invalidate()} so Vaadin's own session-management state stays intact.
     */
    public void reloadPage() {
        com.vaadin.flow.server.WrappedSession session =
                UI.getCurrent().getSession().getSession();
        // Copy the names first to avoid ConcurrentModificationException while removing.
        java.util.Set<String> names = new java.util.HashSet<>(session.getAttributeNames());
        for (String name : names) {
            session.removeAttribute(name);
        }
        UI.getCurrent().getPage().reload();
    }

 public void showCompanies(List<com.sdnah.Ticket_Management_System_.Backend.DTOs.Company.CompanyDTO> companies) {
        companyList.removeAll();

        companyList.add(companies.stream().map(c -> {
            VerticalLayout companyCard = new VerticalLayout();
            companyCard.setAlignItems(Alignment.CENTER);
            companyCard.setPadding(false);
            companyCard.setSpacing(false);
            companyCard.getStyle()
                    .set("width", "150px")
                    .set("cursor", "pointer")
                    .set("transition", "transform 0.2s");

            // Hover effect
            companyCard.getElement().addEventListener("mouseover",
                    e -> companyCard.getStyle().set("transform", "scale(1.05)"));
            companyCard.getElement().addEventListener("mouseout",
                    e -> companyCard.getStyle().set("transform", "scale(1)"));

            // Using the properties from the DTO
           String imageUrl = (c.getLogoURL() != null && !c.getLogoURL().isEmpty()) 
                          ? c.getLogoURL() 
                          : "https://ui-avatars.com/api/?name=" + c.getCompanyName().replace(" ", "+") + "&background=026cdf&color=fff&size=150";

        Image logo = new Image(imageUrl, c.getCompanyName());
            logo.setWidth("80px");
            logo.setHeight("80px");
            
            logo.getStyle()
                    .set("min-width", "100px")  
                    .set("min-height", "100px") 
                    .set("flex-shrink", "0")
                    .set("border-radius", "50%")
                    .set("object-fit", "cover")
                    .set("box-shadow", "0 4px 10px rgba(0,0,0,0.1)");

            H3 name = new H3(c.getCompanyName());
            name.getStyle().set("margin-top", "10px").set("color", "#111827");

            companyCard.add(logo, name, buildRatingStars(c.getRating()));
            companyCard.addClickListener(e -> {
                UI.getCurrent().getSession().setAttribute("companyId", c.getCompanyId().toString());
                UI.getCurrent().navigate("company");
            });

            return companyCard;
            
        }).collect(Collectors.toList()));
    }

    // --------------------------------------------------------------------------

    /** Compact read-only star rating used for both event rows and company cards. */
    private Span buildRatingStars(double rating) {
        Span span = new Span();
        span.getStyle().set("font-size", "13px").set("margin-top", "4px");
        if (rating <= 0) {
            span.setText("No ratings yet");
            span.getStyle().set("color", "#9ca3af");
            return span;
        }
        long rounded = Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= rounded ? "★" : "☆");
        span.setText(sb + "  " + String.format("%.1f", rating));
        span.getStyle().set("color", "#f5a623");
        return span;
    }

    private Div createSectionContainer() {
        Div container = new Div();
        container.getStyle()
                .set("padding", "40px 52px 0 52px")
                .set("width", "100%")
                .set("box-sizing", "border-box");
        return container;
    }

    private Div createBlueHeaderContainer(String titleText) {
        Div headerContainer = new Div();
        headerContainer.getStyle()
                .set("background", "#026cdf")
                .set("padding", "14px 24px")
                .set("border-radius", "8px 8px 0 0");

        H2 header = new H2(titleText);
        header.getStyle()
                .set("color", "Black")
                .set("margin", "0")
                .set("font-size", "22px");

        headerContainer.add(header);
        return headerContainer;
    }

    public String getCurrentToken() {
        String token = "";
        try {
            Object tokenObj = UI.getCurrent().getSession().getAttribute("token");
            if (tokenObj != null) {
                token = tokenObj.toString();
            }
        } catch (Exception e) {
            // Handle any exceptions related to session access
            e.printStackTrace();
        }
        return token;
    }
}