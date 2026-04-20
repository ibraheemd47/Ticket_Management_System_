package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {
    //private static int counter = 0;
    private final UUID itemId;
    private final int ticketId;  // pre-existing ticket from EVENT aggregate
    private final Integer  seatId;    // null if standing area — display only
    private final int areaId;    
    private final BigDecimal price;
    private Lock lock; //null after checkout or expiry

    public OrderItem(int ticketId, Integer seatId, int areaId, BigDecimal price) {
        if (price    == null || price.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("price must be non-negative");
        this.itemId   = UUID.randomUUID();
        this.ticketId = ticketId;
        this.seatId   = seatId;
        this.areaId   = areaId;
        this.price    = price;
        this.lock     = null;
    }
    public int getLockResourceId() {
        return ticketId;
    }
    public boolean isSeated() {
        return seatId != null;
    }
    public void setLock(Lock lock) {
        this.lock=lock;
    }
    public void clearLock() {
        this.lock = null;
    }
    public UUID getItemId(){ 
        return itemId; 
    }
    public int getTicketId(){ 
        return ticketId; 
    }
    public Integer getSeatId(){ 
        return seatId; 
    }
    public int getAreaId(){ 
        return areaId; 
    }
    public BigDecimal getPrice(){ 
        return price; 
    }
    public Lock getLock(){ 
        return lock; 
    }
    
}
