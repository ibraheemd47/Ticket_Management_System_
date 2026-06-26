package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.OwnerAppointmentRequest;

@Repository
public interface OwnerAppointmentRequestRepository
        extends JpaRepository<OwnerAppointmentRequest, UUID> {

    List<OwnerAppointmentRequest> findByCandidateIdAndStatus(
            String candidateId, OwnerAppointmentRequest.Status status);

    List<OwnerAppointmentRequest> findByCompanyIdAndStatus(
            UUID companyId, OwnerAppointmentRequest.Status status);

    Optional<OwnerAppointmentRequest> findByCompanyIdAndCandidateIdAndStatus(
            UUID companyId, String candidateId, OwnerAppointmentRequest.Status status);
}
