package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import jakarta.persistence.*;

@Entity
public class Seat {
    @Transient
    private Logger logger = (Logger) LoggerFactory.getLogger(Seat.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "row_id")
    private Row row; // Each seat belongs to one row
    private String seatNumber;
    private boolean isFilled; // To track if the seat is filled or not

    protected Seat() {}

    public Seat(long id, String seatNumber, Row row) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.row = row;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
    public String getSeatNumber() {
        return seatNumber;
    }
    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
    public boolean Fillseat() {
        if (isFilled) {
            throw new IllegalStateException("Seat is already filled!");
        }
        logger.info("Filling seat {} in row {}", this.seatNumber, this.row.getRowNumber());
        this.isFilled = true; // Mark the seat as filled
        // Implementation for filling the seat with a ticket
        return isFilled;
    }
    public Row getRow() {
        return row;
    }
    public void setRow(Row row) {
        this.row = row;
    }
}

