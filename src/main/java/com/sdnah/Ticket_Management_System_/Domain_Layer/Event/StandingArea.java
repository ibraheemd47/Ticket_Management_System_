package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.Entity;

@Entity
public class StandingArea extends Area {

    private int maxCapacity; // Added because standing areas usually need a limit

    public StandingArea() {}
    // TODO: Implementation of StandingArea class with necessary fields and methods, such as maxCapacity to limit the number of people in the standing area.
}
