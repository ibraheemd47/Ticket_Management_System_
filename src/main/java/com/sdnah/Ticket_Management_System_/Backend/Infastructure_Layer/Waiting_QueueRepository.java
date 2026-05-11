package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Waiting_Queue.WaitingQueue;

@Repository
public interface Waiting_QueueRepository extends JpaRepository<WaitingQueue, Long> {
}
