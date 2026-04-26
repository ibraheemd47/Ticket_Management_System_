package com.sdnah.Ticket_Management_System_.Infastructure_Layer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sdnah.Ticket_Management_System_.DTOs.Waiting_QueueDTO;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Waiting_Queue.WaitingQueue;

public interface Waiting_QueueRepository extends JpaRepository<WaitingQueue, Long> {
    

    public void joinQueue(long userId); 

    public Waiting_QueueDTO findByShowId(long showId);
    public void save(Waiting_QueueDTO waitingQueue);
    public void deleteByShowId(long showId);
    public void deleteByUserId(long userId);


}
