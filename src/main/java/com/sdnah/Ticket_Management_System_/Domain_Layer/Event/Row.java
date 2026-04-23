package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;


import jakarta.persistence.*;
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
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "row_id")
    private List<Seat> seats;

    public Row(long id, String rowNumber, List<Seat> seats) {
        this.id = id;
        this.rowNumber = rowNumber;
        this.seats = seats;
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
}
