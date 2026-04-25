package com.sdnah.Ticket_Management_System_.Infastructure_Layer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.DTOs.TicketDTO;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<ticket, UUID> {

    public Optional<ticket> findById(UUID ticketId) ;
    public void save(TicketDTO ticket) ;
    public List<ticket> findByOwnerId(UUID ownerId) ;

}
