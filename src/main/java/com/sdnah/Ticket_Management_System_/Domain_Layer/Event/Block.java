package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;


import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

@Entity
public class Block {
    @Transient
    private Logger logger = (Logger) LoggerFactory.getLogger(Block.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String blockIdentifier; // e.g., "Section A"

    // Composition: A Block contains 1 to many Rows
    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Row> rows;

    @Transient
    private SeatedArea seatedArea;

    protected Block() {}

    public Block(long id, String blockIdentifier, int numberofRows, SeatedArea seatedArea) {
        this.id = id;
        this.blockIdentifier = blockIdentifier;
        this.rows = new ArrayList<>(numberofRows);
        this.seatedArea = seatedArea;
        this.logger.info("Block created with ID: {}", this.id);
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