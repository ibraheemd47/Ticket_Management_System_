package com.sdnah.Ticket_Management_System_.Domain_Layer.Event;
<<<<<<< HEAD
public interface IEventRepository {
=======
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
>>>>>>> main

@Repository
public interface IEventRepository extends JpaRepository<Event, Long> {
    // TODO : Implement the queries for the Event entity here
    // we still don't know if we have to put the Repository in the domain Layer or insturfracture layer.
}
