package com.sdnah.Ticket_Management_System_.Backend.DTOs;

public class CreateComplaintDTO {
  private String subject;
    private String description;
    private String targetType;
    private String targetId;

    public CreateComplaintDTO() {
    }

    public CreateComplaintDTO(String subject,
                              String description,
                              String targetType,
                              String targetId) {
        this.subject = subject;
        this.description = description;
        this.targetType = targetType;
        this.targetId = targetId;
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
}
