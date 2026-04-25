package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // Keeps all areas in one table for faster queries
public abstract class Area {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // i dont know if we need this but it can be useful for identifying the area
    
    public Area() {}
   // TODO: Implementation of AREA class and its subclasses (Seated_area and Standing_area) with necessary fields and methods.
}