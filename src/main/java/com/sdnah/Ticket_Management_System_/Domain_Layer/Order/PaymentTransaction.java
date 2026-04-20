package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentTransaction {
    private final String transactionId;
    private final int orderId;
    private final BigDecimal amount;
    private Status status;
    private final LocalDateTime timestamp;

    public enum Status {
        PENDING, SUCCESS, FAILED, REFUNDED
    }

    public PaymentTransaction(String transactionId, int orderId, BigDecimal amount, Status status) {
        if (transactionId == null || transactionId.isBlank())
            throw new IllegalArgumentException("transactionId required");
        if (orderId <= 0)
            throw new IllegalArgumentException("orderId required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("amount must be non-negative");
        if (status == null)
            throw new IllegalArgumentException("status required");
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isSuccessful() {
        return status == Status.SUCCESS;
    }

    public boolean isRefunded() {
        return status == Status.REFUNDED;
    }

    public void markRefunded() {
        this.status = Status.REFUNDED;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
