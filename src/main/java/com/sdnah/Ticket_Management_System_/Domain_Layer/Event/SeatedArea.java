package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;


import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

@Entity
public class SeatedArea extends Area {
    @Transient
    private Logger logger = (Logger) LoggerFactory.getLogger(SeatedArea.class);

    // Composition: A Seated Area contains 1 to many Blocks
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "seated_area_id")
    private List<Block> blocks;
    private int NumberofBlocks;

    protected SeatedArea() {}

    public SeatedArea(String name,int NumberofBlocks) {
        super(name);
        this.NumberofBlocks = NumberofBlocks;
        this.blocks = new ArrayList<>(NumberofBlocks); // Initialize the list with the specified number of blocks
    }

    public int getNumberofBlocks() {
        return NumberofBlocks;
    }
    public Block[] getBlocks() {
        logger.info("Retrieving blocks for seated area {}", this.getId());
        return blocks.toArray(new Block[0]);
    }
    
}