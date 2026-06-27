package com.sdnah.Ticket_Management_System_.Backend.DTOs.Company;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Company.CompanyPermission;

/** One pending manager-appointment invite, as the candidate sees it (II.4.7). */
public class ManagerAppointmentRequestDTO {

    private final UUID requestId;
    private final UUID companyId;
    private final String companyName;
    private final String appointerId;
    private final String appointerName;
    private final Set<CompanyPermission> permissions;
    private final LocalDateTime createdAt;

    public ManagerAppointmentRequestDTO(UUID requestId, UUID companyId, String companyName,
                                        String appointerId, String appointerName,
                                        Set<CompanyPermission> permissions,
                                        LocalDateTime createdAt) {
        this.requestId      = requestId;
        this.companyId      = companyId;
        this.companyName    = companyName;
        this.appointerId    = appointerId;
        this.appointerName  = appointerName;
        this.permissions    = permissions;
        this.createdAt      = createdAt;
    }

    public UUID getRequestId()                 { return requestId; }
    public UUID getCompanyId()                 { return companyId; }
    public String getCompanyName()             { return companyName; }
    public String getAppointerId()             { return appointerId; }
    public String getAppointerName()           { return appointerName; }
    public Set<CompanyPermission> getPermissions() { return permissions; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
}
