package pe.victoryordi.paycore.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.victoryordi.paycore.application.port.in.command.CreatePaymentCommand;
import pe.victoryordi.paycore.application.port.out.IdempotencyPort;
import pe.victoryordi.paycore.application.port.out.OutboxPort;
import pe.victoryordi.paycore.application.port.out.PaymentGatewayPort;
import pe.victoryordi.paycore.application.port.out.PaymentRepositoryPort;
import pe.victoryordi.paycore.application.port.out.PaymentTelemetryPort;
import pe.victoryordi.paycore.domain.event.DomainEvent;
import pe.victoryordi.paycore.domain.exception.IdempotencyConflictException;
import pe.victoryordi.paycore.domain.model.Currency;
import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;
import pe.victoryordi.paycore.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

    @Mock
    private PaymentRepositoryPort repository;
    @Mock
    private PaymentGatewayPort gateway;
    @Mock
    private IdempotencyPort idempotency;
    @Mock
    private OutboxPort outbox;
    @Mock
    private PaymentTelemetryPort telemetry;

    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentApplicationService(
                repository,
                gateway,
                idempotency,
                outbox,
                telemetry,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldAuthorizePersistAndPublishPayment() {
        CreatePaymentCommand command = command();
        when(idempotency.reserve(eq("idem-001"), anyString(), any()))
                .thenReturn(IdempotencyPort.Reservation.acquired("lease-001"));
        when(gateway.authorize(any(Payment.class)))
                .thenReturn(PaymentGatewayPort.AuthorizationResult.approved("AUTH-123"));
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = service.create(command, "idem-001");

        assertThat(result.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(result.authorizationCode()).isEqualTo("AUTH-123");
        verify(outbox).append(any(DomainEvent.class));
        verify(telemetry).authorizationCompleted(eq(result), any());
        verify(idempotency).complete(
                eq("idem-001"),
                anyString(),
                eq("lease-001"),
                eq(result.id()),
                any());
    }

    @Test
    void shouldReturnExistingPaymentForIdempotentReplay() {
        UUID paymentId = UUID.randomUUID();
        Payment existing = Payment.create(
                command().merchantId(),
                command().orderId(),
                command().amount(),
                command().paymentToken(),
                command().description(),
                NOW);
        when(idempotency.reserve(eq("idem-001"), anyString(), any()))
                .thenReturn(IdempotencyPort.Reservation.replay(paymentId));
        when(repository.findById(paymentId)).thenReturn(Optional.of(existing));

        Payment result = service.create(command(), "idem-001");

        assertThat(result).isSameAs(existing);
        verify(gateway, never()).authorize(any());
        verify(repository, never()).save(any());
        verify(telemetry).idempotencyReplay();
    }

    @Test
    void shouldRejectIdempotencyKeyReusedWithDifferentPayload() {
        when(idempotency.reserve(eq("idem-001"), anyString(), any()))
                .thenReturn(IdempotencyPort.Reservation.conflict());

        assertThatThrownBy(() -> service.create(command(), "idem-001"))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("different request");

        verify(gateway, never()).authorize(any());
        verify(telemetry).idempotencyConflict();
    }

    @Test
    void shouldCaptureAuthorizedPaymentAndEmitTelemetry() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = authorizedPayment();
        when(repository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(repository.save(payment)).thenReturn(payment);

        Payment result = service.capture(paymentId);

        assertThat(result.status()).isEqualTo(PaymentStatus.CAPTURED);
        verify(gateway).capture(payment);
        verify(outbox).append(any(DomainEvent.class));
        verify(telemetry).paymentCaptured(payment);
    }

    @Test
    void shouldReleaseIdempotencyLeaseWhenAuthorizationFails() {
        when(idempotency.reserve(eq("idem-001"), anyString(), any()))
                .thenReturn(IdempotencyPort.Reservation.acquired("lease-001"));
        when(gateway.authorize(any(Payment.class)))
                .thenThrow(new IllegalStateException("gateway unavailable"));

        assertThatThrownBy(() -> service.create(command(), "idem-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gateway unavailable");

        verify(idempotency).release(eq("idem-001"), anyString(), eq("lease-001"));
        verify(repository, never()).save(any());
        verify(outbox, never()).append(any());
    }

    private Payment authorizedPayment() {
        Payment payment = Payment.create(
                command().merchantId(),
                command().orderId(),
                command().amount(),
                command().paymentToken(),
                command().description(),
                NOW);
        payment.authorize("AUTH-123", NOW);
        return payment;
    }

    private CreatePaymentCommand command() {
        return new CreatePaymentCommand(
                UUID.fromString("8a736e75-4e2b-43d0-a132-fb88dace28cf"),
                "ORDER-001",
                new Money(new BigDecimal("150.00"), Currency.PEN),
                "tok_test_4242",
                "Compra de prueba");
    }
}
