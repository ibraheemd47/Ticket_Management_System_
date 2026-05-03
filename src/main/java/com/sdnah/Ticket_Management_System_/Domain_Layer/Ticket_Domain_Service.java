package com.sdnah.Ticket_Management_System_.Domain_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.ActiveOrder;

public class Ticket_Domain_Service {

    private Event event;
    private ActiveOrder order;

    public Ticket_Domain_Service(Event event , ActiveOrder order) {
       this.event = event;
       this.order = order;
    }

}
