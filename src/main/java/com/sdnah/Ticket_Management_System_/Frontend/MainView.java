package com.sdnah.Ticket_Management_System_.Frontend;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Company.company_managment_serivce;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.UserService;
import com.sdnah.Ticket_Management_System_.Backend.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route("main")
public class MainView extends VerticalLayout {
    private final EventService eventService;
    private final NotificationService notificationService;
    // private final IrepresnteUserService userService;
    private final UserService userService;
    private final company_managment_serivce companyService;
    private Grid<EventDto> eventGrid;
private List<EventDto> allEventDtos;

    public MainView(EventService eventService, NotificationService notificationService,
            UserService userService,company_managment_serivce companyService) {
        this.eventService = eventService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.companyService = companyService;

        // 1. Match the overall page background and spacing from ProfileView
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        setupHeader();
        setupQuickAccessBar(); // links to /queue, /manager/order, /company
        setupEventSection();
        setupCompanySection();
        setupHottestArtistsSection(); // Added Artists Section
        setupFooter(); // Added About Us & Socials Footer
    }

    /**
     * Quick links to the role-specific views. Only rendered when the user is
     * logged in — anonymous visitors have no reason to see these.
     */
    private void setupQuickAccessBar() {
        if (UI.getCurrent().getSession().getAttribute("token") == null) {
            return; // hide the entire bar for anonymous visitors
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
                .set("color", "black") // Changed back to white for contrast on blue
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

            String currentToken = (String) UI.getCurrent().getSession().getAttribute("token");

            // Admin button - only for system admins
            if (currentToken != null && userService.isSystemAdmin(currentToken)) {
                Button adminBtn = new Button("Admin Dashboard",
                        e -> UI.getCurrent().navigate("admin"));

                adminBtn.getStyle()
                        .set("background", "#111827")
                        .set("color", "white")
                        .set("font-weight", "700")
                        .set("border-radius", "8px")
                        .set("cursor", "pointer");

                authButtons.add(adminBtn);
            }

            Button logoutBtn = new Button("Logout", e -> {
                // 1. Get the current token from the session
               // String currentToken = (String) UI.getCurrent().getSession().getAttribute("token");

                // 2. Call your backend UserService logout function
                if (currentToken != null) {
                    userService.logout(currentToken);
                    // Note: If your logout function takes a username instead of a token,
                    // pass the username here!
                }

                // 3. Clear the session attribute so the UI knows the user is logged out
                UI.getCurrent().getSession().setAttribute("token", null);

                // 4. THE FIX: Reload the page to refresh the header buttons
                UI.getCurrent().getPage().reload();
            });
            NotificationBell notifcation_bell = new NotificationBell(notificationService, userService);

            logoutBtn.getStyle().set("background", "transparent").set("color", "white")
                    .set("border", "2px solid white").set("font-weight", "700")
                    .set("border-radius", "8px").set("cursor", "pointer");

            authButtons.add( notifcation_bell, profileBtn, logoutBtn);
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
        eventGrid.addComponentColumn(ev -> {
            Button btn = new Button("View Event", e -> {
                UI.getCurrent().getSession().setAttribute("eventId", ev.id.toString());
                UI.getCurrent().navigate("EventDetails");
            });
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn.getStyle().set("font-size", "13px").set("cursor", "pointer");
            return btn;
        }).setHeader("").setAutoWidth(true);

        try {
           List<Event> events = eventService.getAllEvents();
           
           allEventDtos = events.stream()
    .map(ev -> {
        LocalDate start = ev.getStartDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

        LocalDate end = ev.getEndDate().toInstant()
                          .atZone(ZoneId.systemDefault())
                          .toLocalDate();

        return new EventDto(
            ev.getEventId(),
            ev.getName(),
            start,
            end,
            ev.getEventType(),
            ev.getVenue(),
            ev.getPhotoUrl()
        );
    })
    .collect(Collectors.toList());

eventGrid.setItems(allEventDtos);
        } catch (Exception ignored) {
        }

        eventGrid.setAllRowsVisible(true);
        eventGrid.getStyle().set("border-radius", "0 0 8px 8px");
        container.add(headerContainer, eventGrid);
        add(container);
        // 3. Custom styled buttons for the blue background
        HorizontalLayout authButtons = new HorizontalLayout();
    }

    private void setupCompanySection() {
        Div container = createSectionContainer();
        Div headerContainer = createBlueHeaderContainer("Our Partner Companies");

        HorizontalLayout companyList = new HorizontalLayout();
        companyList.setWidthFull();
        companyList.setSpacing(true);
        companyList.getStyle()
                .set("background", "white")
                .set("padding", "20px")
                .set("border-radius", "0 0 8px 8px");

        // Example Company Mock
         companyList.add(companyService.getActiveCompanies().stream().map(c -> {
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

            Image logo = new Image(c.getLogoURL() != null ? c.getLogoURL() : "https://via.placeholder.com/100?text=No+Logo",
                    c.getCompanyName());
            logo.setWidth("80px");
            logo.setHeight("80px");
            logo.getStyle()
                    .set("border-radius", "50%")
                    .set("object-fit", "cover")
                    .set("box-shadow", "0 4px 10px rgba(0,0,0,0.1)");

            H3 name = new H3(c.getCompanyName());
            name.getStyle().set("margin-top", "10px").set("color", "#111827");

            companyCard.add(logo, name);
            companyCard.addClickListener(e -> {
                UI.getCurrent().getSession().setAttribute("companyId", c.getCompanyId().toString());
                UI.getCurrent().navigate("company");
            });

            return companyCard;

        }).collect(Collectors.toList()));

        container.add(headerContainer, companyList);
        add(container);
        
    }

    // --- NEW: Hottest Artists Section ---
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

        // Add the artists. Ensure you place the actual images in:
        // src/main/resources/META-INF/resources/images/
        // I am using external placeholder URLs temporarily so you can see the layout
        // immediately
        artistList.add(
                createArtistCard("Drake",
                        "https://ui-avatars.com/api/?name=Drake&background=0D8ABC&color=fff&size=150"),
                createArtistCard("J. Cole",
                        "https://ui-avatars.com/api/?name=J+Cole&background=111827&color=fff&size=150"),
                createArtistCard("Kendrick Lamar",
                        "https://ui-avatars.com/api/?name=Kendrick+Lamar&background=E53E3E&color=fff&size=150"),
                createArtistCard("The Weeknd",
                        "https://ui-avatars.com/api/?name=The+Weeknd&background=D69E2E&color=fff&size=150"));

        container.add(headerContainer, artistList);
        add(container);
    }

    // Helper for creating individual clickable artist cards
    private Div createArtistCard(String name, String imageUrl) {
        Div card = new Div();
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("cursor", "pointer") // Makes mouse pointer a hand
                .set("transition", "transform 0.2s");

        // Hover effect for the card
        card.getElement().addEventListener("mouseover", e -> card.getStyle().set("transform", "scale(1.05)"));
        card.getElement().addEventListener("mouseout", e -> card.getStyle().set("transform", "scale(1)"));

        Image img = new Image(imageUrl, name);
        img.setWidth("100px");
        img.setHeight("100px");
        img.getStyle()
                .set("border-radius", "50%") // Makes image a perfect circle
                .set("object-fit", "cover")
                .set("box-shadow", "0 4px 10px rgba(0,0,0,0.1)");

        H3 artistName = new H3(name);
        artistName.getStyle().set("margin-top", "15px").set("color", "#111827");

        card.add(img, artistName);

        // Navigation listener
        card.addClickListener(e -> {
            // e.g., UI.getCurrent().navigate("search?artist=" + name);
            System.out.println("Navigating to shows for: " + name);
        });

        return card;
    }

    // --- NEW: About Us & Socials Footer ---
    private void setupFooter() {
        Div footer = new Div();
        footer.setWidthFull();
        footer.getStyle()
                .set("background", "#111827") // Dark modern background
                .set("color", "white")
                .set("padding", "50px 52px")
                .set("margin-top", "60px") // Pushes footer down
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("box-sizing", "border-box");

        // About Us Left Side
        Div aboutSection = new Div();
        aboutSection.setWidth("50%");
        H2 aboutTitle = new H2("About VibePass");
        aboutTitle.getStyle().set("margin-top", "0").set("color", "white");

        Paragraph aboutDesc = new Paragraph(
                "VibePass is your ultimate gateway to live entertainment. We connect fans directly with the artists, teams, and events they love. Our mission is to provide a seamless, fair, and incredibly secure ticketing experience so you can focus on enjoying the show.");
        aboutDesc.getStyle().set("line-height", "1.6").set("color", "#9ca3af");
        aboutSection.add(aboutTitle, aboutDesc);

        // Social Media Right Side
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

    // Helper to create styled, clickable social icons
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

    searchInput.addValueChangeListener(e -> {
        String value = e.getValue() != null ? e.getValue().trim() : "";

        if (value.isEmpty()
                && startDate.getValue() == null
                && endDate.getValue() == null
                && eventGrid != null
                && allEventDtos != null) {
            eventGrid.setItems(allEventDtos);
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

    AtomicReference<String> activeFilter = new AtomicReference<>("Event");

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

        performModernSearch(activeFilter.get(), searchInput.getValue(), start, end);
    });

    searchWrapper.add(pills, searchBox);
    return searchWrapper;
}

// Helper to style the tab pills
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
private void performModernSearch(String filterMode, String query, Date startDate, Date endDate) {
    String text = query != null ? query.trim() : "";
    List<EventDto> results = null;

    try {
        if (text.isEmpty() && startDate == null && endDate == null) {
            if (allEventDtos != null) {
                eventGrid.setItems(allEventDtos);
            }
            return;
        }

        if (filterMode.equals("Event")) {
            if (!text.isEmpty()) {
                results = eventService.searchEventByName(text);
            } else if (startDate != null && endDate != null) {
                results = eventService.searchEventsByDateRange(startDate, endDate);
            } else if (startDate != null) {
                results = eventService.searchEventsByStartDate(startDate);
            } else if (endDate != null) {
                results = eventService.searchEventsByEndDate(endDate);
            }
        } else if (filterMode.equals("Company")) {
            if (!text.isEmpty()) {
                if (startDate != null && endDate != null) {
                    results = companyService.searchEventsInCompanyByDateRange(text, startDate, endDate);
                } else if (startDate != null) {
                    results = companyService.searchEventsInCompanyByStartDate(text, startDate);
                } else if (endDate != null) {
                    results = companyService.searchEventsInCompanyByEndDate(text, endDate);
                } else {
                    results = companyService.searchEventsInCompanyByKeyword(text, text);
                }
            }
        } else if (filterMode.equals("Venue")) {
            if (!text.isEmpty()) {
                results = eventService.searchEventsByVenue(text);
            }
        }

        if (results != null && !results.isEmpty()) {
            eventGrid.setItems(results);
        } else {
            eventGrid.setItems();
            Notification.show("No events were found matching your search.", 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
        }

    } catch (Exception ex) {
        Notification.show("Search Error: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);

        System.err.println("Search Error: " + ex.getMessage());
        ex.printStackTrace();
    }
}

    // private Div createSectionContainer() {
    // Div container = new Div();
    // container.getStyle()
    // .set("padding", "40px 52px 0 52px")
    // .set("width", "100%")
    // .set("box-sizing", "border-box");
    // return container;
    // }

    // authButtons.add(loginBtn, signupBtn);
    // } else {

    // H2 header = new H2(titleText);
    // header.getStyle()
    // .set("color", "white") // Fixed to white so it looks good on the blue
    // background
    // .set("margin", "0")
    // .set("font-size", "22px");

    // NotificationBell notificationBell = new NotificationBell(notificationService,
    // userService);
    // profileBtn.getStyle()
    // .set("background", "white")
    // .set("color", "#026cdf")
    // .set("font-weight", "700")
    // .set("border-radius", "8px")
    // .set("cursor", "pointer");

    // Button logoutBtn = new Button("Logout", e -> {
    // UI.getCurrent().getSession().setAttribute("token", null);
    // UI.getCurrent().navigate("main");
    // });

    // logoutBtn.getStyle()
    // .set("background", "transparent")
    // .set("color", "white")
    // .set("border", "2px solid white")
    // .set("font-weight", "700")
    // .set("border-radius", "8px")
    // .set("cursor", "pointer");

    // authButtons.add(notificationBell, profileBtn, logoutBtn);//<= to add if need
    // in others

    // // authButtons.add(profileBtn, logoutBtn);

    // header.add(logo, searchField, authButtons);
    // header.expand(searchField);

    // add(header);

    private Div createSectionContainer() {
        Div container = new Div();
        container.getStyle()
                .set("padding", "40px 52px 0 52px")
                .set("width", "100%")
                .set("box-sizing", "border-box");
        return container;
    }

    // --- Helper Methods for Consistent Styling ---

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
}