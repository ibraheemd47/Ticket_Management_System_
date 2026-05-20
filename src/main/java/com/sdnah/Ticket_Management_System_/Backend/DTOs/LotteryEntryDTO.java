package com.sdnah.Ticket_Management_System_.Backend.DTOs;

import java.time.LocalDateTime;
import java.util.UUID;

public class LotteryEntryDTO {

    private UUID id;
    private UUID lotteryId;
    private String memberId;
    private boolean winner;
    private LocalDateTime registeredAt;
    private String accessCode;           // null אם לא זכה
    private LocalDateTime accessCodeExpiresAt;

    public LotteryEntryDTO() {}

    public LotteryEntryDTO(UUID id, UUID lotteryId, String memberId,
                           boolean winner, LocalDateTime registeredAt,
                           String accessCode, LocalDateTime accessCodeExpiresAt) {
        this.id = id;
        this.lotteryId = lotteryId;
        this.memberId = memberId;
        this.winner = winner;
        this.registeredAt = registeredAt;
        this.accessCode = accessCode;
        this.accessCodeExpiresAt = accessCodeExpiresAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLotteryId() { return lotteryId; }
    public void setLotteryId(UUID lotteryId) { this.lotteryId = lotteryId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public boolean isWinner() { return winner; }
    public void setWinner(boolean winner) { this.winner = winner; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }

    public LocalDateTime getAccessCodeExpiresAt() { return accessCodeExpiresAt; }
    public void setAccessCodeExpiresAt(LocalDateTime accessCodeExpiresAt) { this.accessCodeExpiresAt = accessCodeExpiresAt; }
}