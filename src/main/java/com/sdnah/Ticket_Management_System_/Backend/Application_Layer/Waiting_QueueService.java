package com.sdnah.Ticket_Management_System_.Backend.Application_Layer;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Waiting_Queue.WaitingQueue;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.Waiting_QueueRepository;

import ch.qos.logback.classic.Logger;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class Waiting_QueueService {

    @Autowired
    private Waiting_QueueRepository waitingQueueRepository;
    private final Logger logger = (Logger) LoggerFactory.getLogger(Waiting_QueueService.class);

    public Waiting_QueueService(Waiting_QueueRepository waitingQueueRepository) {
        this.waitingQueueRepository = waitingQueueRepository;
    }

    public boolean joinQueue(long userId, long showId) {
        logger.info("User {} joining waiting queue for show {}", userId, showId);
        WaitingQueue queue = waitingQueueRepository.findById(showId)
                .orElseGet(() -> new WaitingQueue(showId, 50));
        boolean result = queue.joinQueue(userId);
        waitingQueueRepository.save(queue);
        return result;
    }

    public int getPosition(long userId, long showId) {
        logger.info("Getting position for user {} in waiting queue for show {}", userId, showId);
        return waitingQueueRepository.findById(showId)
                .map(q -> q.getPosition(userId))
                .orElse(-1);
    }

    public int calculateEstimatedWaitTimeInMinutes(long userId, long showId) {
        logger.info("Calculating estimated wait time for user {} in waiting queue for show {}", userId, showId);
        return waitingQueueRepository.findById(showId)
                .map(q -> q.calculateEstimatedWaitTimeInMinutes(userId))
                .orElse(-1);
    }

    public List<Long> admitNextUsers(int amountToAdmit, long showId) {
        logger.info("Admitting next {} users from waiting queue for show {}", amountToAdmit, showId);
        return waitingQueueRepository.findById(showId)
                .map(q -> {
                    List<Long> admitted = q.admitNextUsers(amountToAdmit);
                    waitingQueueRepository.save(q);
                    return admitted;
                })
                .orElse(List.of());
    }

    public void clearQueue(long showId) {
        logger.info("Clearing waiting queue for show {}", showId);
        waitingQueueRepository.findById(showId).ifPresent(q -> {
            q.clearQueue();
            waitingQueueRepository.save(q);
        });
    }
}
