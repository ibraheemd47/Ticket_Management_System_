package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Complaint;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.ComplaintStatus;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {


    List<Complaint> findByReporterMemberId(String reporterMemberId);

    List<Complaint> findByStatus(ComplaintStatus status);

}
