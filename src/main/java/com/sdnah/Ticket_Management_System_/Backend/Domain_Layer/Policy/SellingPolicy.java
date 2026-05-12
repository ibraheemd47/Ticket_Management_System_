package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy;
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

        if (type == null) {
            throw new IllegalArgumentException("Selling type cannot be null");
        }

        this.type = type;
    }

    @Override
    public boolean isValid() {
        return type != null;
    }

    public boolean isSelectionAllowed(boolean isMember) {
        if (type == null) {
            return false;
        }

        return type == SellingType.REGULAR || isMember;
    }

    public SellingType getType() {
        return type;
    }

    public void setType(SellingType type) {
        if (type == null) {
            throw new IllegalArgumentException("Selling type cannot be null");
        }

        this.type = type;
    }
}