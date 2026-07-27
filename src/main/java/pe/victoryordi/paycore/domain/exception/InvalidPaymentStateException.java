package pe.victoryordi.paycore.domain.exception;

import pe.victoryordi.paycore.domain.model.PaymentStatus;

import java.util.UUID;

public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(UUID paymentId, PaymentStatus status, String operation) {
        super("Payment %s in status %s cannot %s".formatted(paymentId, status, operation));
    }
}
