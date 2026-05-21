package com.sdnah.Ticket_Management_System_.Backend.DTOs;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.Complaint;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.User.ComplaintStatus;



public class ComplaintDTO {

    private UUID complaintId;
    private String reporterMemberId;
    private String subject;
    private String description;
    private String targetType;
    private String targetId;
    private ComplaintStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String adminResponse;

    public ComplaintDTO(Complaint complaint) {
        this.complaintId = complaint.getComplaintId();
        this.reporterMemberId = complaint.getReporterMemberId();
        this.subject = complaint.getSubject();
        this.description = complaint.getDescription();
        this.targetType = complaint.getTargetType();
        this.targetId = complaint.getTargetId();
        this.status = complaint.getStatus();
        this.createdAt = complaint.getCreatedAt();
        this.resolvedAt = complaint.getResolvedAt();
        this.adminResponse = complaint.getAdminResponse();
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

    @Override
    public String toString() {
        return "Complaint{" +
                "id=" + complaintId +
                ", reporter='" + reporterMemberId + '\'' +
                ", subject='" + subject + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
