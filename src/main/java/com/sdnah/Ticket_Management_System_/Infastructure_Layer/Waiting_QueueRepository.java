package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import com.sdnah.Ticket_Management_System_.Domain_Layer.Waiting_Queue.WaitingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Waiting_QueueRepository extends JpaRepository<WaitingQueue, Long> {
}
