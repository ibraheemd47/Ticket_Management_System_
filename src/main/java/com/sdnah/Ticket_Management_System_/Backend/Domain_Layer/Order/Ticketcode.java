package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order;

public class Ticketcode {
    private final String code;
    private final String qrData;

    public Ticketcode(String code, String qrData) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be empty");
        }
        if (qrData == null || qrData.isBlank()) {
            throw new IllegalArgumentException("qrData must not be empty");
        }
        this.code = code;
        this.qrData = qrData;
    }

    public String getCode() {
        return code;
    }

    public String getQrData() {
        return qrData;
    }

}
