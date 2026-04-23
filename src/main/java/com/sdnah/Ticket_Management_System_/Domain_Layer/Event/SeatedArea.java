package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;


import jakarta.persistence.*;
import java.util.List;

@Entity
public class SeatedArea extends Area {

    // Composition: A Seated Area contains 1 to many Blocks
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "seated_area_id")
    private List<Block> blocks;

    public SeatedArea() {}
    // TODO: Add Getters and Setters
}