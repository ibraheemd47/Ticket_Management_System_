package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery;

@Repository
public interface LotteryRepository extends JpaRepository<Lottery, UUID> {

    List<Lottery> findByEventId(UUID eventId);

    List<Lottery> findByCompanyId(int companyId);
}