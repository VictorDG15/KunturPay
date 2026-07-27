package pe.victoryordi.paycore.domain.model;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    DECLINED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
