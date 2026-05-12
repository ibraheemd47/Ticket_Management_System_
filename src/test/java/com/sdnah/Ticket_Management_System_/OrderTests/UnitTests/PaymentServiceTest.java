package com.sdnah.Ticket_Management_System_.OrderTests.UnitTests;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.PaymentService;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentDetails;
import com.sdnah.Ticket_Management_System_.Backend.Domain_Layer.Order.PaymentTransaction;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentTransactionRepository;

class PaymentServiceTest {

    @Test
    void constructor_shouldThrow_whenRepositoryIsNull() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentService(null)
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "transactionRepo required",
                ex.getMessage()
        );
    }

    @Test
    void saveTransaction_shouldSaveTransactionInRepository() {

        PaymentTransactionRepository transactionRepository =
                mock(PaymentTransactionRepository.class);

        PaymentService paymentService =
                new PaymentService(transactionRepository);

        PaymentTransaction tx = mock(PaymentTransaction.class);

        when(tx.getTransactionId()).thenReturn("tx123");

        paymentService.saveTransaction(tx);

        verify(transactionRepository).save(tx);
    }
}