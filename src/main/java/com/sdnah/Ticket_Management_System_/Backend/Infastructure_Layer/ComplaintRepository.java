package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;

public interface ComplaintRepository extends JpaRepository<ComplaintDTO, Long> {
    



}
