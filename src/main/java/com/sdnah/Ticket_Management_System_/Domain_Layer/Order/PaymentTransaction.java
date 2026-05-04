package com.sdnah.Ticket_Management_System_.Domain_Layer.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    public enum Status {
        PENDING, SUCCESS, FAILED, REFUNDED
    }

    protected PaymentTransaction() {
    }

    public PaymentTransaction(String transactionId, UUID orderId, BigDecimal amount, Status status) {
        if (transactionId == null || transactionId.isBlank())
            throw new IllegalArgumentException("transactionId required");
        if (orderId == null)
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

    public UUID getOrderId() {
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