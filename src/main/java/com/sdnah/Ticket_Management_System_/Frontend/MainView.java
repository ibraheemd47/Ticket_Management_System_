package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.List;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route("main") // Landing page
public class MainView extends VerticalLayout {
    private final EventService eventService;

    public MainView(EventService eventService) {
        this.eventService = eventService;

        // 1. Match the overall page background and spacing from ProfileView
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle()
                .set("background", "#f4f4f4")
                .set("font-family", "Arial, sans-serif");

        setupHeader();
        setupEventSection();    
        setupCompanySection();
        setupHottestArtistsSection(); // Added Artists Section
        setupFooter(); // Added About Us & Socials Footer
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
                .set("color", "white") // Changed back to white for contrast on blue
                .set("margin", "0")
                .set("font-size", "24px")
                .set("font-weight", "900")
                .set("cursor", "pointer");
        logo.addClickListener(e -> UI.getCurrent().navigate(MainView.class));

        TextField searchField = setupSearchBar();
        searchField.getStyle().set("margin", "0 40px"); 

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

            Button logoutBtn = new Button("Logout", e -> {
                UI.getCurrent().getSession().setAttribute("token", null);
                UI.getCurrent().navigate("main");
            });
            logoutBtn.getStyle().set("background", "transparent").set("color", "white")
                    .set("border", "2px solid white").set("font-weight", "700")
                    .set("border-radius", "8px").set("cursor", "pointer");

            authButtons.add(profileBtn, logoutBtn);
        }
        
        header.add(logo, searchField, authButtons);
        header.expand(searchField); 
        add(header);
    }

    private void setupEventSection() {
        Div container = createSectionContainer();
        Div headerContainer = createBlueHeaderContainer("Upcoming Events");

        Grid<Event> eventGrid = new Grid<>(Event.class, false);
        eventGrid.addColumn(Event::getName).setHeader("Event Name").setFlexGrow(2);
        eventGrid.addColumn(ev -> ev.getVenue() != null ? ev.getVenue() : "—")
                .setHeader("Venue").setFlexGrow(2);
        eventGrid.addColumn(ev -> ev.getEventType() != null ? ev.getEventType().name() : "—")
                .setHeader("Type").setFlexGrow(1);
        eventGrid.addComponentColumn(ev -> {
            Button btn = new Button("View Event", e -> {
                UI.getCurrent().getSession().setAttribute("eventId", ev.getEventId().toString());
                UI.getCurrent().navigate("EventDetails");
            });
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn.getStyle().set("font-size", "13px").set("cursor", "pointer");
            return btn;
        }).setHeader("").setAutoWidth(true);

        try {
            List<Event> events = eventService.getAllEvents();
            eventGrid.setItems(events);
        } catch (Exception ignored) {}

        eventGrid.setAllRowsVisible(true);
        eventGrid.getStyle().set("border-radius", "0 0 8px 8px");

        container.add(headerContainer, eventGrid);
        add(container);
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
        // companyList.add(new H3("LiveNation"), new H3("SeatGeek"), new H3("StubHub"));

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

        // Add the artists. Ensure you place the actual images in: src/main/resources/META-INF/resources/images/
        // I am using external placeholder URLs temporarily so you can see the layout immediately
        artistList.add(
            createArtistCard("Drake", "https://ui-avatars.com/api/?name=Drake&background=0D8ABC&color=fff&size=150"),
            createArtistCard("J. Cole", "https://ui-avatars.com/api/?name=J+Cole&background=111827&color=fff&size=150"),
            createArtistCard("Kendrick Lamar", "https://ui-avatars.com/api/?name=Kendrick+Lamar&background=E53E3E&color=fff&size=150"),
            createArtistCard("The Weeknd", "https://ui-avatars.com/api/?name=The+Weeknd&background=D69E2E&color=fff&size=150")
        );

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
            "VibePass is your ultimate gateway to live entertainment. We connect fans directly with the artists, teams, and events they love. Our mission is to provide a seamless, fair, and incredibly secure ticketing experience so you can focus on enjoying the show."
        );
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
            createSocialIcon(VaadinIcon.YOUTUBE)
        );

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

    private TextField setupSearchBar() {
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search for artists, Areas, and events");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidth("50%"); 
        
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            // Backend search logic
        });
        return searchField;
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
                .set("color", "white") // Fixed to white so it looks good on the blue background
                .set("margin", "0")
                .set("font-size", "22px");

        headerContainer.add(header);
        return headerContainer;
    }
}