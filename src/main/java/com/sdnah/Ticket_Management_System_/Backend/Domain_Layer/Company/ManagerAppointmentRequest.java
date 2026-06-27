package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * II.4.7 — Manager appointment request. Mirrors
 * {@link OwnerAppointmentRequest} but carries the proposed permission set
 * so the candidate sees what they're being asked to do before they accept.
 */
@Entity
@Table(name = "manager_appointment_requests", indexes = {
        @Index(name = "idx_mgr_req_candidate", columnList = "candidateId,status"),
        @Index(name = "idx_mgr_req_company",   columnList = "companyId,status")
})
public class ManagerAppointmentRequest {

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

    @Convert(converter = CompanyPermissionSetConverter.class)
    @Column(name = "permissions", length = 1024)
    private Set<CompanyPermission> permissions = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    protected ManagerAppointmentRequest() {}

    public ManagerAppointmentRequest(UUID companyId, String candidateId,
                                     String appointerId, Set<CompanyPermission> permissions) {
        if (companyId    == null) throw new IllegalArgumentException("companyId required");
        if (candidateId  == null || candidateId.isBlank()) throw new IllegalArgumentException("candidateId required");
        if (appointerId  == null || appointerId.isBlank()) throw new IllegalArgumentException("appointerId required");
        this.id           = UUID.randomUUID();
        this.companyId    = companyId;
        this.candidateId  = candidateId.trim();
        this.appointerId  = appointerId.trim();
        this.permissions  = permissions == null ? new HashSet<>() : new HashSet<>(permissions);
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

    public UUID getId()                       { return id; }
    public UUID getCompanyId()                { return companyId; }
    public String getCandidateId()            { return candidateId; }
    public String getAppointerId()            { return appointerId; }
    public Set<CompanyPermission> getPermissions() {
        return permissions == null ? Set.of() : Set.copyOf(permissions);
    }
    public Status getStatus()                 { return status; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getRespondedAt()     { return respondedAt; }
}
