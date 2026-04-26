package com.sdnah.Ticket_Management_System_.Application_Layer;
import java.util.List;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.DTOs.Waiting_QueueDTO;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Event.ticket;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Waiting_Queue.QueueEntry;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Waiting_Queue.WaitingQueue;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.TicketRepository;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.Waiting_QueueRepository;

import ch.qos.logback.classic.Logger;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class Waiting_QueueService {
    @Autowired
    private Waiting_QueueRepository WaitingQueueRepository;
    private final Logger logger = (Logger) LoggerFactory.getLogger(Waiting_QueueService.class);

   
      public boolean JoinQueue(long userId,long showId) {
        boolean result = false;
        // Logic to add the user to the waiting queue for the specified show
        logger.info("User {} joined the waiting queue for show {}", userId);
        Waiting_QueueDTO waitingQueue =  WaitingQueueRepository.findByShowId(showId);
        if (waitingQueue == null) {
           WaitingQueue waitingQueue1 = new WaitingQueue(showId, 50);
           
        WaitingQueueRepository.joinQueue(userId);
        result = waitingQueue1.joinQueue(userId);
        }
            return result;
    }
    public int getPosition(long userId,long showId) {
        // Logic to get the user's position in the waiting queue for the specified show
        logger.info("Getting position for user {} in waiting queue for show {}", userId, showId);
        Waiting_QueueDTO waitingQueue =  WaitingQueueRepository.findByShowId(showId);
        WaitingQueue waitingQueue1 = new WaitingQueue(showId, 50);
        if (waitingQueue == null) {
            return -1; // or throw an exception
        }
        return waitingQueue1.getPosition(userId);
    }
    public int calculateEstimatedWaitTimeInMinutes(long userId,long showId) {
        // Logic to calculate the estimated wait time for the user based on their position in the queue and the checkout capacity
        logger.info("Calculating estimated wait time for user {} in waiting queue for show {}", userId, showId);
        Waiting_QueueDTO waitingQueue =  WaitingQueueRepository.findByShowId(showId);
        WaitingQueue waitingQueue1 = new WaitingQueue(showId, 50);
        if (waitingQueue == null) {
            return -1; // or throw an exception
        }
        return waitingQueue1.calculateEstimatedWaitTimeInMinutes(userId);
    }
    public List<Long> admitNextUsers(int amountToAdmit,long showId) {
        // Logic to admit the next set of users from the waiting queue to the checkout system based on the defined capacity
        logger.info("Admitting next {} users from waiting queue for show {}", amountToAdmit, showId);
        Waiting_QueueDTO waitingQueue =  WaitingQueueRepository.findByShowId(showId);
        WaitingQueue waitingQueue1 = new WaitingQueue(showId, 50);
        if (waitingQueue == null) {
            return null; // or throw an exception
        }
        return waitingQueue1.admitNextUsers(amountToAdmit);
    }
    public void clearQueue(long showId) {
        // Logic to clear the waiting queue for a show, typically after tickets are released or the show is sold out
        Waiting_QueueDTO waitingQueue =  WaitingQueueRepository.findByShowId(showId);
        WaitingQueue waitingQueue1 = new WaitingQueue(showId, 50);
        if (waitingQueue != null) {
            logger.info("Clearing waiting queue for show {}", showId);
            waitingQueue1.clearQueue();
            WaitingQueueRepository.deleteByShowId(showId); // Clear the queue from the repository as well
        }
    }
}


