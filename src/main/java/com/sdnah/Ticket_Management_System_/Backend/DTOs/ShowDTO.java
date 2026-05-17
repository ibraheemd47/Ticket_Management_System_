package com.sdnah.Ticket_Management_System_.Backend.DTOs;
import java.time.LocalDate;
import java.util.UUID;
public class ShowDTO {
    public String name;
    public LocalDate showDate;
    public String eventType;
    public String description;
    public String singer;
    public UUID eventId;

    // Standing area
    public int standingCapacity; // 0 = no standing area

    // Seated area (all three must be > 0 to configure)
    public int numBlocks;
    public int rowsPerBlock;
    public int seatsPerRow;

    public ShowDTO(UUID EventId, String name, String description, String singer, LocalDate showDate) {
        this.eventId = EventId;
        this.name = name;
        this.description = description;
        this.singer = singer;
        this.showDate = showDate;
    }

    public int totalSeatedCapacity() {
        return numBlocks > 0 ? numBlocks * rowsPerBlock * seatsPerRow : 0;
    }

    public int totalCapacity() {
        return standingCapacity + totalSeatedCapacity();
    }
}
   

