package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;


import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
// "Row" is a reserved keyword in some SQL databases, so we rename the table just in case!
@Table(name = "seating_row") 
public class Row {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rowNumber;

    // Composition: A Row contains 1 to many Seats
    @OneToMany(mappedBy = "row", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats;

    // Links back to the parent Block
    @ManyToOne
    @JoinColumn(name = "block_id")
    private Block block;

    protected Row() {}

    public Row(long id, String rowNumber, int numberOfSeats, Block block) {
        this.id = id;
        this.rowNumber = rowNumber;
        this.seats = new ArrayList<>(numberOfSeats);
        this.block = block;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
    public String getRowNumber() {
        return rowNumber;
    }
    public void setRowNumber(String rowNumber) {
        this.rowNumber = rowNumber;
    }
    public List<Seat> getSeats() {
        return seats;
    }
    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }
    public Block getBlock() {
        return block;
    }
    public void setBlock(Block block) {
        this.block = block;
    }
    
}
