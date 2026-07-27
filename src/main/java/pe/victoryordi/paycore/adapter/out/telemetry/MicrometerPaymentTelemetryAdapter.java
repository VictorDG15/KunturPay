package pe.victoryordi.paycore.adapter.out.telemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import pe.victoryordi.paycore.application.port.out.PaymentTelemetryPort;
import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

import java.time.Duration;

@Component
public class MicrometerPaymentTelemetryAdapter implements PaymentTelemetryPort {

    private final MeterRegistry registry;

    public MicrometerPaymentTelemetryAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void authorizationCompleted(Payment payment, Duration duration) {
        Counter.builder("payments.authorization.completed")
                .description("Payment authorization results")
                .tag("status", payment.status().name())
                .tag("currency", payment.amount().currency().name())
                .register(registry)
                .increment();

        Timer.builder("payments.authorization.duration")
                .description("Time spent authorizing a payment")
                .tag("status", payment.status().name())
                .publishPercentileHistogram()
                .register(registry)
                .record(duration);
    }

    @Override
    public void paymentCaptured(Payment payment) {
        Counter.builder("payments.capture.completed")
                .description("Successfully captured payments")
                .tag("currency", payment.amount().currency().name())
                .register(registry)
                .increment();
    }

    @Override
    public void paymentRefunded(Payment payment, Money refund) {
        Counter.builder("payments.refund.completed")
                .description("Successfully completed refunds")
                .tag("resulting_status", payment.status().name())
                .tag("currency", refund.currency().name())
                .register(registry)
                .increment();

        DistributionSummary.builder("payments.refund.amount")
                .description("Refund amount in the payment currency")
                .baseUnit("currency_units")
                .tag("currency", refund.currency().name())
                .register(registry)
                .record(refund.amount().doubleValue());
    }

    @Override
    public void idempotencyReplay() {
        registry.counter("payments.idempotency.replay").increment();
    }

    @Override
    public void idempotencyConflict() {
        registry.counter("payments.idempotency.conflict").increment();
    }
}
