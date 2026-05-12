package com.sdnah.Ticket_Management_System_.Frontend;

import com.vaadin.flow.theme.lumo.LumoUtility;

import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.s;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.EventService;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route("") // Landing page
public class MainView extends VerticalLayout {
    private final EventService eventService; // Application service

    public MainView(EventService eventService) {
        this.eventService = eventService;
        setupHeader();
        setupSearchBar();
       
    }

private void setupHeader() {
    // 1. Logo on the left
    H1 logo = new H1("TICKET MANAGEMENT");
    logo.getStyle().set("margin", "0");
    logo.getStyle().set("font-size", "var(--lumo-font-size-l)");
    logo.addClickListener(e -> UI.getCurrent().navigate(MainView.class));

    // 2. The Search Bar (The middle part)
    // TextField searchField = new TextField();
    // searchField.setPlaceholder("Search for artists, venues, and events");
    // searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    // searchField.setClearButtonVisible(true);
    
    // // Make it wider - use a CSS width or a percentage
    // searchField.setWidth("50%"); 
    TextField searchField = setupSearchBar();

    // 3. Login Button on the right
    Button loginBtn = new Button("Login", e -> UI.getCurrent().navigate("login"));
    loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button signupBtn = new Button("Sign Up", e -> UI.getCurrent().navigate("signup"));
    signupBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

    // 4. Put them all in a HorizontalLayout
    HorizontalLayout header = new HorizontalLayout(logo, searchField, loginBtn, signupBtn);
    
    // 5. Layout Magic: This makes the search bar stay in the middle and take up space
    header.setWidthFull();
    header.setAlignItems(Alignment.CENTER);
    header.setPadding(true);
    
    // This pushes the logo left and the button right
    header.setJustifyContentMode(JustifyContentMode.BETWEEN);
    
    // If you want the search bar even wider, tell the header to "expand" it
    header.expand(searchField); 

    add(header);
}


private TextField setupSearchBar() {
    TextField searchField = new TextField();
    searchField.setPlaceholder("Search for artists, Areas, and events");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setClearButtonVisible(true);
    
    // Make it wider - use a CSS width or a percentage
    searchField.setWidth("50%"); 
    
    // Trigger search on every keystroke (with a small delay for performance)
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(e -> {
        // Here you call your backend logic to filter the events
        // updateEventList(e.getValue()); // to implement in a future step
    });
    return searchField;
    
}


}
