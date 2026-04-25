<<<<<<< HEAD
package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;
=======
package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Event {

 
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // And you use it exactly the same way
    @Enumerated(EnumType.STRING)
    private show_type eventType;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<show> shows;

    public Event() {}
}
>>>>>>> main
