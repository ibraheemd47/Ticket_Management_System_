package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Lottery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "lotteries")
public class Lottery {
    

    @Id
    private UUID id;

    private UUID eventId;
    private UUID companyId;

    private LocalDateTime registrationDeadline;
    private LocalDateTime drawTime;
    private LocalDateTime openSaleTime;   // ← when exclusivity ends and anyone can buy


    @Enumerated(EnumType.STRING)
    private LotteryStatus status;
    
    @OneToMany(mappedBy = "lottery", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LotteryEntry> entries = new ArrayList<>();

    public enum LotteryStatus {
        OPEN,       // OPEN TO REGISTRATION
        DRAWN,      // DRAWN BUT NOT CLOSED YET
        CLOSED      // CLOSED FOR ANY ACTIONS
    }


    protected Lottery() {}

    public Lottery(UUID eventId, UUID companyId,
                   LocalDateTime registrationDeadline,
                   LocalDateTime drawTime) {
        if (eventId == null) throw new IllegalArgumentException("eventId cannot be null");
        if (companyId ==null) throw new IllegalArgumentException("companyId must be positive");
        if (registrationDeadline == null) throw new IllegalArgumentException("registrationDeadline cannot be null");
        if (drawTime == null) throw new IllegalArgumentException("drawTime cannot be null");
        if (!drawTime.isAfter(registrationDeadline)) throw new IllegalArgumentException("drawTime must be after registrationDeadline");

        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.companyId = companyId;
        this.registrationDeadline = registrationDeadline;
        this.drawTime = drawTime;
        this.status = LotteryStatus.OPEN;
    }

    // register member for lottery — returns the created entry
    public LotteryEntry register(String memberId) {
        if (memberId == null || memberId.isBlank())
            throw new IllegalArgumentException("memberId cannot be null");
        if (status != LotteryStatus.OPEN)
            throw new IllegalStateException("Lottery is not open for registration");
        if (LocalDateTime.now().isAfter(registrationDeadline))
            throw new IllegalStateException("Registration deadline has passed");
        if (isAlreadyRegistered(memberId))
            throw new IllegalStateException("Member is already registered in this lottery");

        LotteryEntry entry = new LotteryEntry(this, memberId);
        entries.add(entry);
        return entry;
    }

    public List<LotteryEntry> draw(int winnersCount) {
        return draw(winnersCount, LocalDateTime.now().plusHours(24));
    }

    public List<LotteryEntry> draw(int winnersCount, LocalDateTime openSaleTime) {
        if (status != LotteryStatus.OPEN)
            throw new IllegalStateException("Lottery is not open");
        if (LocalDateTime.now().isBefore(registrationDeadline))
            throw new IllegalStateException("Cannot draw before the registration deadline has passed");
        if (winnersCount <= 0)
            throw new IllegalArgumentException("winnersCount must be positive");
        if (openSaleTime == null || openSaleTime.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("openSaleTime must be in the future");

        List<LotteryEntry> shuffled = new ArrayList<>(entries);
        Collections.shuffle(shuffled);
        int count = Math.min(winnersCount, shuffled.size());
        List<LotteryEntry> winners = shuffled.subList(0, count);

        winners.forEach(e -> e.markAsWinner(openSaleTime));
        this.status = LotteryStatus.DRAWN;
        this.openSaleTime = openSaleTime;
        return winners;
    }
    
  
    // public List<LotteryEntry> draw(int winnersCount) {
    //     return draw(winnersCount, java.time.Duration.ofHours(24));   // default, keeps old callers working
    // }

    // public List<LotteryEntry> draw(int winnersCount, java.time.Duration exclusiveWindow) {
    //     if (status != LotteryStatus.OPEN)
    //         throw new IllegalStateException("Lottery is not open");
    //     if (LocalDateTime.now().isBefore(registrationDeadline))
    //         throw new IllegalStateException("Cannot draw before the registration deadline has passed");
    //     if (winnersCount <= 0)
    //         throw new IllegalArgumentException("winnersCount must be positive");
    //     if (exclusiveWindow == null || exclusiveWindow.isZero() || exclusiveWindow.isNegative())
    //         throw new IllegalArgumentException("exclusiveWindow must be positive");

    //     List<LotteryEntry> shuffled = new ArrayList<>(entries);
    //     Collections.shuffle(shuffled);
    //     int count = Math.min(winnersCount, shuffled.size());
    //     List<LotteryEntry> winners = shuffled.subList(0, count);

    //     winners.forEach(e -> e.markAsWinner(exclusiveWindow));   // ← same window for every winner's code
    //     this.status = LotteryStatus.DRAWN;
    //     //this.openSaleTime = LocalDateTime.now().plus(exclusiveWindow);   // ← and for the public open-sale moment
    //     this.openSaleTime = openSaleTime;
    //     return winners;
    // }


    public boolean isAlreadyRegistered(String memberId) {
        return entries.stream()
                .anyMatch(e -> e.getMemberId().equals(memberId));
    }

    public boolean isOpen() {
        return status == LotteryStatus.OPEN
                && LocalDateTime.now().isBefore(registrationDeadline);
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getCompanyId() { return companyId; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public LocalDateTime getDrawTime() { return drawTime; }
    public LotteryStatus getStatus() { return status; }
    public List<LotteryEntry> getEntries() { return Collections.unmodifiableList(entries); }
    public LocalDateTime getOpenSaleTime() { return openSaleTime; }
}