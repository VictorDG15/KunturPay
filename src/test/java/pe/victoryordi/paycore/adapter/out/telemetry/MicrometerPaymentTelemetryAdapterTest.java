package pe.victoryordi.paycore.adapter.out.telemetry;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.victoryordi.paycore.domain.model.Currency;
import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerPaymentTelemetryAdapterTest {

    private SimpleMeterRegistry registry;
    private MicrometerPaymentTelemetryAdapter telemetry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        telemetry = new MicrometerPaymentTelemetryAdapter(registry);
    }

    @Test
    void shouldExposeLowCardinalityAuthorizationMetrics() {
        Payment payment = authorizedPayment();

        telemetry.authorizationCompleted(payment, Duration.ofMillis(25));

        assertThat(registry.get("payments.authorization.completed")
                .tag("status", "AUTHORIZED")
                .tag("currency", "PEN")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.get("payments.authorization.duration")
                .tag("status", "AUTHORIZED")
                .timer()
                .totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isEqualTo(25);
    }

    @Test
    void shouldExposeRefundAndIdempotencyMetrics() {
        Payment payment = authorizedPayment();
        payment.capture(Instant.parse("2026-07-26T00:00:01Z"));
        Money refund = new Money(new BigDecimal("40.00"), Currency.PEN);
        payment.refund(refund, Instant.parse("2026-07-26T00:00:02Z"));

        telemetry.paymentRefunded(payment, refund);
        telemetry.idempotencyReplay();
        telemetry.idempotencyConflict();

        assertThat(registry.get("payments.refund.completed")
                .tag("resulting_status", "PARTIALLY_REFUNDED")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.get("payments.refund.amount").summary().totalAmount())
                .isEqualTo(40);
        assertThat(registry.get("payments.idempotency.replay").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("payments.idempotency.conflict").counter().count())
                .isEqualTo(1);
    }

    private Payment authorizedPayment() {
        Instant now = Instant.parse("2026-07-26T00:00:00Z");
        Payment payment = Payment.create(
                UUID.randomUUID(),
                "ORDER-001",
                new Money(new BigDecimal("150.00"), Currency.PEN),
                "tok_test_4242",
                "Compra de prueba",
                now);
        payment.authorize("AUTH-123", now);
        return payment;
    }
}
