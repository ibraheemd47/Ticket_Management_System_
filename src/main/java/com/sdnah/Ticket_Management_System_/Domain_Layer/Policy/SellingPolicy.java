package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("SELLING")
public class SellingPolicy extends Policy {

    public enum SellingType {
        REGULAR,
        LOTTERY
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "selling_type")
    private SellingType type;

    protected SellingPolicy() {
        // JPA
    }

    public SellingPolicy(int policyId, String description, SellingType type, UUID eventId) {
        super(policyId, description, eventId);
        this.type = type;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public boolean isSelectionAllowed(boolean isMember) {
        return type == SellingType.LOTTERY ? isMember : true;
    }

    public SellingType getType() {
        return type;
    }

    public void setType(SellingType type) {
        this.type = type;
    }
}