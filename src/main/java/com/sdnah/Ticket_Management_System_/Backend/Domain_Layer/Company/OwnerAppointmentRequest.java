package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * II.4.8 — Owner appointment request. Created by an existing owner; the
 * candidate must explicitly accept or reject before they become a real
 * owner. Until then they appear in nobody's role list.
 *
 * <p>One row per request; superseded requests stay around as historical
 * records (status REJECTED / ACCEPTED) — useful audit + lets us prevent
 * spamming the same candidate.
 */
@Entity
@Table(name = "owner_appointment_requests", indexes = {
        @Index(name = "idx_owner_req_candidate", columnList = "candidateId,status"),
        @Index(name = "idx_owner_req_company",   columnList = "companyId,status")
})
public class OwnerAppointmentRequest {

    public enum Status { PENDING, ACCEPTED, REJECTED, CANCELLED }

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "candidate_id", nullable = false, length = 64)
    private String candidateId;

    @Column(name = "appointer_id", nullable = false, length = 64)
    private String appointerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    protected OwnerAppointmentRequest() {}

    public OwnerAppointmentRequest(UUID companyId, String candidateId, String appointerId) {
        if (companyId    == null)                        throw new IllegalArgumentException("companyId required");
        if (candidateId  == null || candidateId.isBlank()) throw new IllegalArgumentException("candidateId required");
        if (appointerId  == null || appointerId.isBlank()) throw new IllegalArgumentException("appointerId required");
        this.id           = UUID.randomUUID();
        this.companyId    = companyId;
        this.candidateId  = candidateId.trim();
        this.appointerId  = appointerId.trim();
        this.status       = Status.PENDING;
        this.createdAt    = LocalDateTime.now();
    }

    public void accept() {
        if (status != Status.PENDING) throw new IllegalStateException("request is not pending");
        status = Status.ACCEPTED;
        respondedAt = LocalDateTime.now();
    }

    public void reject() {
        if (status != Status.PENDING) throw new IllegalStateException("request is not pending");
        status = Status.REJECTED;
        respondedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (status != Status.PENDING) throw new IllegalStateException("request is not pending");
        status = Status.CANCELLED;
        respondedAt = LocalDateTime.now();
    }

    public UUID getId()              { return id; }
    public UUID getCompanyId()       { return companyId; }
    public String getCandidateId()   { return candidateId; }
    public String getAppointerId()   { return appointerId; }
    public Status getStatus()        { return status; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
}
