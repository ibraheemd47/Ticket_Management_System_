package com.sdnah.Ticket_Management_System_.Backend.DTOs;

public class Waiting_QueueDTO {
    public long showId;
    public long userId;

    public Waiting_QueueDTO() {}

    public Waiting_QueueDTO(long showId, long userId) {
        this.showId = showId;
        this.userId = userId;
    }

}
