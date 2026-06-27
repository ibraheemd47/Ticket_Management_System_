package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.ManagerAppointmentRequest;

@Repository
public interface ManagerAppointmentRequestRepository
        extends JpaRepository<ManagerAppointmentRequest, UUID> {

    List<ManagerAppointmentRequest> findByCandidateIdAndStatus(
            String candidateId, ManagerAppointmentRequest.Status status);

    Optional<ManagerAppointmentRequest> findByCompanyIdAndCandidateIdAndStatus(
            UUID companyId, String candidateId, ManagerAppointmentRequest.Status status);
}
