package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.sdnah.Ticket_Management_System_.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Infastructure_Layer.PaymentTransactionRepository;

class PaymentServiceTest {

    @Mock
    private IPaymentGateway paymentGateway;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(paymentGateway, transactionRepository);
    }

    @Test
    void charge_shouldCallGatewayAndSaveTransaction() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100");
        PaymentDetails details = new PaymentDetails("token123", "buyer1", "VISA");

        PaymentTransaction tx = new PaymentTransaction(
                "tx123",
                orderId,
                amount,
                PaymentTransaction.Status.SUCCESS
        );

        when(paymentGateway.charge(orderId, amount, details)).thenReturn(tx);

        PaymentTransaction result = paymentService.charge(orderId, amount, details);

        assertEquals(tx, result);
        verify(paymentGateway).charge(orderId, amount, details);
        verify(transactionRepository).save(tx);
    }

    @Test
    void refund_shouldCallGatewayAndSaveTransaction() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100");
        String transactionId = "tx123";

        PaymentTransaction tx = new PaymentTransaction(
                transactionId,
                orderId,
                amount,
                PaymentTransaction.Status.REFUNDED
        );

        when(paymentGateway.refund(transactionId)).thenReturn(tx);

        PaymentTransaction result = paymentService.refund(transactionId);

        assertEquals(tx, result);
        verify(paymentGateway).refund(transactionId);
        verify(transactionRepository).save(tx);
    }

    @Test
    void constructor_shouldThrow_whenGatewayIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentService(null, transactionRepository)
        );

        assertEquals("paymentGateway required", ex.getMessage());
    }

    @Test
    void constructor_shouldThrow_whenRepositoryIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentService(paymentGateway, null)
        );

        assertEquals("transactionRepo required", ex.getMessage());
    }
}