package com.sdnah.Ticket_Management_System_.Infastructure_Layer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.DTOs.EventDto;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.Event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IEventRepository extends JpaRepository<Event, UUID> {
    public Optional<Event> findById(UUID id);
    public void save(EventDto event);
    public void deleteById(UUID id);
    public List<Event> findAll();

}
