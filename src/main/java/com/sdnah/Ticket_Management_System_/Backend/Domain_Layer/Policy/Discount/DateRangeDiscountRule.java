package com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Policy.Discount;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("DATE_RANGE")
public class DateRangeDiscountRule extends DiscountRule {

    @Column(name = "percentage")
    private double percentage;

    @Column(name = "range_from")
    private LocalDateTime from;   // null = no lower bound

    @Column(name = "range_until")
    private LocalDateTime until;  // null = no upper bound

    protected DateRangeDiscountRule() {}

    public DateRangeDiscountRule(double percentage, LocalDateTime from, LocalDateTime until) {
        if (percentage < 0 || percentage > 100)
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        if (from != null && until != null && from.isAfter(until))
            throw new IllegalArgumentException("'from' must not be after 'until'");
        this.percentage = percentage;
        this.from       = from;
        this.until      = until;
    }

    public static DateRangeDiscountRule before(double pct, LocalDateTime deadline) {
        return new DateRangeDiscountRule(pct, null, deadline);
    }

    public static DateRangeDiscountRule after(double pct, LocalDateTime start) {
        return new DateRangeDiscountRule(pct, start, null);
    }

    @Override
    public double apply(DiscountContext context) {
        LocalDateTime t = context.getPurchaseTime();
        if (from  != null && t.isBefore(from))  return 0.0;
        if (until != null && t.isAfter(until))   return 0.0;
        return percentage;
    }

    @Override
    public String describe() {
        if (from == null && until != null)
            return percentage + "% discount for purchases up to " + until;
        if (from != null && until == null)
            return percentage + "% discount for purchases from " + from;
        return percentage + "% discount for purchases from " + from + " until " + until;
    }

    public double        getPercentage() { return percentage; }
    public LocalDateTime getFrom()       { return from; }
    public LocalDateTime getUntil()      { return until; }
}