package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Event;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import java.util.LinkedList;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
@Entity
public class StandingArea extends Area {
    @Transient
    private Logger logger = (Logger) LoggerFactory.getLogger(StandingArea.class);

    private int maxCapacity;
    @Transient
    private LinkedList<ticket> areaMap;
    
    protected StandingArea() {}

    public StandingArea(String name, int maxCapacity) {
        super(name);
        this.maxCapacity = maxCapacity;
        this.areaMap = new LinkedList<ticket>();
    }

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
        logger.info("Checking if standing area {} is full", this.getId());
        return areaMap.size() >= maxCapacity;
    }
    public int getCurrentCapacity() {
        logger.info("Retrieving current capacity for standing area {}", this.getId());
        return areaMap.size();
    }
    public boolean removeTicket(ticket t) {
        logger.info("Removing ticket from standing area {}", this.getId()); 
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
