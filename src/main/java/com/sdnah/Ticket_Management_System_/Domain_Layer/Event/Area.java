package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.*;

import java.util.UUID; // Import the UUID class

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Area {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Specifically tells JPA to use a UUID
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    private Logger logger = (Logger) LoggerFactory.getLogger(Area.class);

    private String name;

    public Area(String name) {
        this.id = UUID.randomUUID(); // Generate a new UUID when an Area is created
        this.name = name;
        this.logger.info("Area created with ID: {}", this.id);
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