package com.sdnah.Ticket_Management_System_.Backend.DTOs.Company;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One pending owner-appointment invite, as the candidate sees it (II.4.8).
 */
public class OwnerAppointmentRequestDTO {

    private final UUID requestId;
    private final UUID companyId;
    private final String companyName;
    private final String appointerId;
    private final String appointerName;
    private final LocalDateTime createdAt;

    public OwnerAppointmentRequestDTO(UUID requestId, UUID companyId, String companyName,
                                      String appointerId, String appointerName,
                                      LocalDateTime createdAt) {
        this.requestId      = requestId;
        this.companyId      = companyId;
        this.companyName    = companyName;
        this.appointerId    = appointerId;
        this.appointerName  = appointerName;
        this.createdAt      = createdAt;
    }

    public UUID getRequestId()           { return requestId; }
    public UUID getCompanyId()           { return companyId; }
    public String getCompanyName()       { return companyName; }
    public String getAppointerId()       { return appointerId; }
    public String getAppointerName()     { return appointerName; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
