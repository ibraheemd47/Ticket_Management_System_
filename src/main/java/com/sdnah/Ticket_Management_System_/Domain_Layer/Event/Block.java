package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;
import jakarta.persistence.*;

import java.util.ArrayList;
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

    private SeatedArea seatedArea;

    public Block(long id, String blockIdentifier, int numberofRows, SeatedArea seatedArea) {
        this.id = id;
        this.blockIdentifier = blockIdentifier;
        this.rows = new ArrayList<>(numberofRows);
        this.seatedArea = seatedArea;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
    public String getBlockIdentifier() {
        return blockIdentifier;
    }
    public void setBlockIdentifier(String blockIdentifier) {
        this.blockIdentifier = blockIdentifier;
    }
    public List<Row> getRows() {
        return rows;
    }
    public void setRows(List<Row> rows) {
        this.rows = rows;
    }
    public SeatedArea getSeatedArea() {
        return seatedArea;
    }   
}