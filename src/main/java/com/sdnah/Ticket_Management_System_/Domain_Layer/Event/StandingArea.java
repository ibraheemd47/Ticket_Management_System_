package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.Entity;
import java.util.LinkedList;
import java.util.UUID;
@Entity
public class StandingArea extends Area {

    private int maxCapacity; // Added because standing areas usually need a limit
    private LinkedList<ticket> areaMap ;
    
    public StandingArea(String name, int maxCapacity) {
        super(name);
        this.maxCapacity = maxCapacity;
        this.areaMap = new LinkedList<ticket>();
    }
    // TODO: Implementation of StandingArea class with necessary fields and methods, such as maxCapacity to limit the number of people in the standing area.
    public boolean addTicket(ticket t) {
        if (areaMap.size() < maxCapacity) {
            areaMap.add(t);
            return true;
        }
        return false; // Can't add more tickets, standing area is at full capacity
    }
    public UUID getId() {
        return super.getId();
    }
    public boolean isFull() {
        return areaMap.size() >= maxCapacity;
    }
    public int getCurrentCapacity() {
        return areaMap.size();
    }
    public boolean removeTicket(ticket t) {
        return areaMap.remove(t);
    }
    public LinkedList<ticket> getAreaMap() {
        return areaMap;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }


}
