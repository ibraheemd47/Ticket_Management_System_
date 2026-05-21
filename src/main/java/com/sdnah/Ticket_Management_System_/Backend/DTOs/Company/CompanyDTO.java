package com.sdnah.Ticket_Management_System_.Backend.DTOs.Company;

import java.util.UUID;

public class CompanyDTO {

    private final UUID companyId;
    private final String companyName;
    private final boolean isOpen;
    private final double rating;
    private final String logoURL;

    public CompanyDTO(UUID companyId,
                      String companyName,
                      boolean isOpen,
                      double rating,
                      String logoURL) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.isOpen = isOpen;
        this.rating = rating;
        this.logoURL = logoURL;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public double getRating() {
        return rating;
    }

    public String getLogoURL() {
        return logoURL;
    }
}