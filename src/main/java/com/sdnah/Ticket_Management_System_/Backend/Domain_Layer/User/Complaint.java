package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.checkerframework.checker.units.qual.C;

import com.sdnah.Ticket_Management_System_.Backend.DTOs.ComplaintDTO;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    private UUID complaintId;

    private String reporterMemberId;

    private String subject;

    @Column(length = 2000)
    private String description;

    private String targetType;

    private String targetId;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @Column(length = 2000)
    private String adminResponse;

    protected Complaint() {
    }

    public Complaint(String reporterMemberId,
                     String subject,
                     String description,
                     String targetType,
                     String targetId) {
        if (reporterMemberId == null || reporterMemberId.isBlank()) {
            throw new IllegalArgumentException("Reporter member id is required");
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Complaint subject is required");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Complaint description is required");
        }

        this.complaintId = UUID.randomUUID();
        this.reporterMemberId = reporterMemberId.trim();
        this.subject = subject.trim();
        this.description = description.trim();
        this.targetType = targetType == null ? null : targetType.trim();
        this.targetId = targetId == null ? null : targetId.trim();
        this.status = ComplaintStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }
    public Complaint(ComplaintDTO dto) {
        this.complaintId = dto.getComplaintId() != null ? dto.getComplaintId() : UUID.randomUUID();
        this.reporterMemberId = dto.getReporterMemberId();
        this.subject = dto.getSubject();
        this.description = dto.getDescription();
        this.targetType = dto.getTargetType();
        this.targetId = dto.getTargetId();
        this.status = dto.getStatus() != null ? dto.getStatus() : ComplaintStatus.OPEN;
        this.createdAt = dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now();
        this.resolvedAt = dto.getResolvedAt();
        this.adminResponse = dto.getAdminResponse();
    }

    public void markInProgress() {
        this.status = ComplaintStatus.IN_PROGRESS;
    }

    public void resolve(String adminResponse) {
        this.status = ComplaintStatus.RESOLVED;
        this.adminResponse = adminResponse;
        this.resolvedAt = LocalDateTime.now();
    }

    public void reject(String adminResponse) {
        this.status = ComplaintStatus.REJECTED;
        this.adminResponse = adminResponse;
        this.resolvedAt = LocalDateTime.now();
    }

    public UUID getComplaintId() {
        return complaintId;
    }

    public String getReporterMemberId() {
        return reporterMemberId;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getAdminResponse() {
        return adminResponse;
    }
    public ComplaintDTO toDTO() {
        return new ComplaintDTO(this);
    }
}