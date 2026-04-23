package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.*;

@Entity
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String seatNumber;
    private boolean isFilled; // To track if the seat is filled or not

    public Seat(long id, String seatNumber) {
        this.id = id;
        this.seatNumber = seatNumber;
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
    public void Fillseat() {
        if (isFilled) {
            throw new IllegalStateException("Seat is already filled!");
        }
        this.isFilled = true; // Mark the seat as filled
        // Implementation for filling the seat with a ticket
    }
}

