package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

// @Entity
// @Table(name = "lottery_entries")

// @Table(name = "lottery_entries", 
// uniqueConstraints = @UniqueConstraint(columnNames = {"lottery_id", "member_id"}))


@Entity
@Table(name = "lottery_entries")
public class LotteryEntry {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_id", nullable = false)
    private Lottery lottery;

    @Column(nullable = false)
    private String memberId;

    private boolean winner;

    private LocalDateTime registeredAt;

    // access code for winners to claim their tickets
    private String accessCode;
    private LocalDateTime accessCodeExpiresAt;

    protected LotteryEntry() {}

    public LotteryEntry(Lottery lottery, String memberId) {
        this.id = UUID.randomUUID();
        this.lottery = lottery;
        this.memberId = memberId;
        this.winner = false;
        this.registeredAt = LocalDateTime.now();
    }

    public void markAsWinner() {
        this.winner = true;
        this.accessCode = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        this.accessCodeExpiresAt = LocalDateTime.now().plusHours(24);
    }

    public boolean isAccessCodeValid() {
        if (!winner || accessCode == null) return false;
        return LocalDateTime.now().isBefore(accessCodeExpiresAt);
    }

    // Getters
    public UUID getId() { return id; }
    public String getMemberId() { return memberId; }
    public boolean isWinner() { return winner; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public String getAccessCode() { return accessCode; }
    public LocalDateTime getAccessCodeExpiresAt() { return accessCodeExpiresAt; }
    public Lottery getLottery() { return lottery; }
}