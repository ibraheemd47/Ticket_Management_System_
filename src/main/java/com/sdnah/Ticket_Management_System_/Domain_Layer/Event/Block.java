package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;
import jakarta.persistence.*;
import java.util.List;

@Entity
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String blockIdentifier; // e.g., "Section A"

    // Composition: A Block contains 1 to many Rows
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "block_id")
    private List<Row> rows;

    public Block() {}
    // TODO: Add Getters and Setters
}