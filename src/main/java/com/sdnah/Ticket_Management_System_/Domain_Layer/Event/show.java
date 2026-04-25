package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links back to the parent Event
    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    // A show has 1 to many Areas
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "show_id") 
    private List<Area> areas;

    public show() {}
    // TODO: Add Getters and Setters
}