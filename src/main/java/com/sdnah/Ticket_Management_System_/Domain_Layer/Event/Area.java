package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.*;

import java.util.UUID; // Import the UUID class

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Area {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Specifically tells JPA to use a UUID
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private String name;

    public Area(String name) {
        this.id = UUID.randomUUID(); // Generate a new UUID when an Area is created
        this.name = name;
       
    }

    // Updated getter to return UUID
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}