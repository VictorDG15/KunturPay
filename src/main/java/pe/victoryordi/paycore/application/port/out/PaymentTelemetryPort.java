package pe.victoryordi.paycore.application.port.out;

import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

import java.time.Duration;

/**
 * Low-cardinality business telemetry exposed by the payment use cases.
 *
 * <p>The application depends on this port instead of Micrometer so the core
 * remains independent from the observability vendor.</p>
 */
public interface PaymentTelemetryPort {

    void authorizationCompleted(Payment payment, Duration duration);

    void paymentCaptured(Payment payment);

    void paymentRefunded(Payment payment, Money refund);

    void idempotencyReplay();

    void idempotencyConflict();
}
