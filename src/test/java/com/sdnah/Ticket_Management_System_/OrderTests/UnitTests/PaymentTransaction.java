package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;
import java.math.BigDecimal;
import java.util.UUID;

public class PaymentTransaction {

    public enum Status {
        SUCCESS,
        FAILED,
        REFUNDED
    }

    private final String transactionId;
    private final UUID orderId;
    private final BigDecimal amount;
    private Status status;

    public PaymentTransaction(String transactionId, UUID orderId, BigDecimal amount, Status status) {

        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId must not be empty");
        }

        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        this.transactionId = transactionId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
    }

    public boolean isSuccessful() {
        return status == Status.SUCCESS;   // 🔥 THIS FIXES YOUR TEST
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

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }
}