package com.sdnah.Ticket_Management_System_.Frontend;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Notifications.NotificationService;
import com.sdnah.Ticket_Management_System_.Frontend.NotificationBell;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route("main") // Landing page
public class MainView extends VerticalLayout {
        private final EventService eventService;
        private final NotificationService notificationService;
        private final IrepresnteUserService userService;

        public MainView(EventService eventService, NotificationService notificationService,
                        IrepresnteUserService userService) {
                this.eventService = eventService;
                this.notificationService = notificationService;
                this.userService = userService;

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
        }

        private void setupHeader() {
                HorizontalLayout header = new HorizontalLayout();

                // 2. Match the blue header style
                header.getStyle()
                                .set("background", "#026cdf")
                                .set("padding", "20px 52px")
                                .set("width", "100%")
                                .set("box-sizing", "border-box");
                header.setAlignItems(Alignment.CENTER);
                header.setJustifyContentMode(JustifyContentMode.BETWEEN);

                H1 logo = new H1("TICKET MANAGEMENT");
                logo.getStyle()
                                .set("color", "Black")
                                .set("margin", "0")
                                .set("font-size", "24px")
                                .set("font-weight", "900")
                                .set("cursor", "pointer");
                logo.addClickListener(e -> UI.getCurrent().navigate(MainView.class));

                TextField searchField = setupSearchBar();
                searchField.getStyle().set("margin", "0 40px"); // Add breathing room

                // 3. Custom styled buttons for the blue background
                HorizontalLayout authButtons = new HorizontalLayout();

                Object token = UI.getCurrent()
                                .getSession()
                                .getAttribute("token");

                if (token == null) {

                        Button loginBtn = new Button("Login",
                                        e -> UI.getCurrent().navigate("login"));

                        loginBtn.getStyle()
                                        .set("background", "white")
                                        .set("color", "#026cdf")
                                        .set("font-weight", "700")
                                        .set("border-radius", "8px")
                                        .set("cursor", "pointer");

                        Button signupBtn = new Button("Sign Up",
                                        e -> UI.getCurrent().navigate("signup"));

                        signupBtn.getStyle()
                                        .set("background", "transparent")
                                        .set("color", "white")
                                        .set("border", "2px solid white")
                                        .set("font-weight", "700")
                                        .set("border-radius", "8px")
                                        .set("cursor", "pointer");

                        authButtons.add(loginBtn, signupBtn);
                } else {

                        NotificationBell notificationBell = new NotificationBell(notificationService, userService);
                        Button profileBtn = new Button("My Profile",
                                        e -> UI.getCurrent().navigate("profile"));

                        profileBtn.getStyle()
                                        .set("background", "white")
                                        .set("color", "#026cdf")
                                        .set("font-weight", "700")
                                        .set("border-radius", "8px")
                                        .set("cursor", "pointer");

                        Button logoutBtn = new Button("Logout", e -> {
                                UI.getCurrent().getSession().setAttribute("token", null);
                                UI.getCurrent().navigate("main");
                        });

                        logoutBtn.getStyle()
                                        .set("background", "transparent")
                                        .set("color", "white")
                                        .set("border", "2px solid white")
                                        .set("font-weight", "700")
                                        .set("border-radius", "8px")
                                        .set("cursor", "pointer");

                        authButtons.add(notificationBell, profileBtn, logoutBtn);//<= to add if need in others

                        // authButtons.add(profileBtn, logoutBtn);
                }
                header.add(logo, searchField, authButtons);
                header.expand(searchField);

                add(header);
        }

        private void setupEventSection() {
                Div container = createSectionContainer();

                Div headerContainer = createBlueHeaderContainer("Upcoming Events");

                Grid<Event> eventGrid = new Grid<>(Event.class, false);
                eventGrid.addColumn(Event::getName).setHeader("Event Name");
                eventGrid.addColumn(Event::getVenue).setHeader("Area");
                // eventGrid.setItems(eventService.getShowsForThisWeek());

                // Round bottom corners to attach smoothly to the blue header
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
                                .set("border-radius", "0 0 8px 8px"); // White box below the blue header

                container.add(headerContainer, companyList);
                add(container);
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

        // --- Helper Methods for Consistent Styling ---

        private Div createSectionContainer() {
                Div container = new Div();
                container.getStyle()
                                .set("padding", "40px 52px 0 52px") // Matches header side padding
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
}