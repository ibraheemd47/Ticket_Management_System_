package com.sdnah.Ticket_Management_System_.Domain_Layer.Policy.Purchase;

import java.time.LocalDateTime;

/**
 * Immutable value object passed through the Composite rule tree.
 * Add new fields here when new rule types need additional context.
 */
public final class PurchaseContext {

    private final int           buyerAge;
    private final int           ticketQuantity;
    private final boolean       createsSingleSeatGap;
    private final boolean       isMember;
    private final LocalDateTime purchaseTime;

    public PurchaseContext(int buyerAge,
                           int ticketQuantity,
                           boolean createsSingleSeatGap,
                           boolean isMember,
                           LocalDateTime purchaseTime) {
        if (buyerAge      < 0)  throw new IllegalArgumentException("buyerAge cannot be negative");
        if (ticketQuantity <= 0) throw new IllegalArgumentException("ticketQuantity must be positive");
        this.buyerAge            = buyerAge;
        this.ticketQuantity      = ticketQuantity;
        this.createsSingleSeatGap = createsSingleSeatGap;
        this.isMember            = isMember;
        this.purchaseTime        = purchaseTime != null ? purchaseTime : LocalDateTime.now();
    }

    public int           getBuyerAge()            { return buyerAge; }
    public int           getTicketQuantity()       { return ticketQuantity; }
    public boolean       isCreatesSingleSeatGap()  { return createsSingleSeatGap; }
    public boolean       isMember()                { return isMember; }
    public LocalDateTime getPurchaseTime()          { return purchaseTime; }
}