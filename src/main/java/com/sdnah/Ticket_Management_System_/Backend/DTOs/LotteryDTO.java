package com.sdnah.Ticket_Management_System_.Backend.DTOs;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery.Lottery.LotteryStatus;

public class LotteryDTO {

    private UUID id;
    private UUID eventId;
    private UUID companyId;
    private LocalDateTime registrationDeadline;
    private LocalDateTime drawTime;
    private LotteryStatus status;
    private int entryCount;

    public LotteryDTO() {}

    public LotteryDTO(UUID id, UUID eventId, UUID companyId,
                      LocalDateTime registrationDeadline,
                      LocalDateTime drawTime,
                      LotteryStatus status,
                      int entryCount) {
        this.id = id;
        this.eventId = eventId;
        this.companyId = companyId;
        this.registrationDeadline = registrationDeadline;
        this.drawTime = drawTime;
        this.status = status;
        this.entryCount = entryCount;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    public LocalDateTime getDrawTime() { return drawTime; }
    public void setDrawTime(LocalDateTime drawTime) { this.drawTime = drawTime; }

    public LotteryStatus getStatus() { return status; }
    public void setStatus(LotteryStatus status) { this.status = status; }

    public int getEntryCount() { return entryCount; }
    public void setEntryCount(int entryCount) { this.entryCount = entryCount; }
}