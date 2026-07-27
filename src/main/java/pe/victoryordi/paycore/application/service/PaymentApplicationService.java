package pe.victoryordi.paycore.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.victoryordi.paycore.application.port.in.CreatePaymentUseCase;
import pe.victoryordi.paycore.application.port.in.ManagePaymentUseCase;
import pe.victoryordi.paycore.application.port.in.QueryPaymentUseCase;
import pe.victoryordi.paycore.application.port.in.command.CreatePaymentCommand;
import pe.victoryordi.paycore.application.port.out.IdempotencyPort;
import pe.victoryordi.paycore.application.port.out.OutboxPort;
import pe.victoryordi.paycore.application.port.out.PaymentGatewayPort;
import pe.victoryordi.paycore.application.port.out.PaymentPage;
import pe.victoryordi.paycore.application.port.out.PaymentRepositoryPort;
import pe.victoryordi.paycore.application.port.out.PaymentTelemetryPort;
import pe.victoryordi.paycore.domain.event.PaymentEventFactory;
import pe.victoryordi.paycore.domain.exception.IdempotencyConflictException;
import pe.victoryordi.paycore.domain.exception.PaymentNotFoundException;
import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PaymentApplicationService
        implements CreatePaymentUseCase, ManagePaymentUseCase, QueryPaymentUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentApplicationService.class);
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(2);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final IdempotencyPort idempotency;
    private final OutboxPort outbox;
    private final PaymentTelemetryPort telemetry;
    private final Clock clock;

    public PaymentApplicationService(
            PaymentRepositoryPort paymentRepository,
            PaymentGatewayPort paymentGateway,
            IdempotencyPort idempotency,
            OutboxPort outbox,
            PaymentTelemetryPort telemetry,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.idempotency = idempotency;
        this.outbox = outbox;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Payment create(CreatePaymentCommand command, String idempotencyKey) {
        String fingerprint = fingerprint(command);
        IdempotencyPort.Reservation reservation =
                idempotency.reserve(idempotencyKey, fingerprint, PROCESSING_TTL);

        if (reservation.status() == IdempotencyPort.ReservationStatus.REPLAY) {
            telemetry.idempotencyReplay();
            return get(reservation.paymentId());
        }
        if (reservation.status() == IdempotencyPort.ReservationStatus.CONFLICT) {
            telemetry.idempotencyConflict();
            throw new IdempotencyConflictException(
                    "Idempotency-Key was already used with a different request");
        }
        if (reservation.status() == IdempotencyPort.ReservationStatus.IN_PROGRESS) {
            telemetry.idempotencyConflict();
            throw new IdempotencyConflictException(
                    "A request with this Idempotency-Key is still being processed");
        }

        try {
            Instant now = clock.instant();
            Payment payment = Payment.create(
                    command.merchantId(),
                    command.orderId(),
                    command.amount(),
                    command.paymentToken(),
                    command.description(),
                    now);

            long authorizationStartedAt = System.nanoTime();
            PaymentGatewayPort.AuthorizationResult authorization = paymentGateway.authorize(payment);
            Duration authorizationDuration =
                    Duration.ofNanos(System.nanoTime() - authorizationStartedAt);
            if (authorization.approved()) {
                payment.authorize(authorization.authorizationCode(), now);
            } else {
                payment.decline(authorization.declineReason(), now);
            }

            Payment saved = paymentRepository.save(payment);
            outbox.append(PaymentEventFactory.authorizationCompleted(saved, now));
            telemetry.authorizationCompleted(saved, authorizationDuration);
            completeIdempotencyAfterCommit(
                    idempotencyKey,
                    fingerprint,
                    reservation.leaseToken(),
                    saved.id());
            return saved;
        } catch (RuntimeException exception) {
            idempotency.release(idempotencyKey, fingerprint, reservation.leaseToken());
            throw exception;
        }
    }

    @Override
    @Transactional
    public Payment capture(UUID paymentId) {
        Payment payment = get(paymentId);
        paymentGateway.capture(payment);
        Instant now = clock.instant();
        payment.capture(now);
        Payment saved = paymentRepository.save(payment);
        outbox.append(PaymentEventFactory.captured(saved, now));
        telemetry.paymentCaptured(saved);
        return saved;
    }

    @Override
    @Transactional
    public Payment refund(UUID paymentId, Money amount) {
        Payment payment = get(paymentId);
        paymentGateway.refund(payment, amount);
        Instant now = clock.instant();
        payment.refund(amount, now);
        Payment saved = paymentRepository.save(payment);
        outbox.append(PaymentEventFactory.refunded(saved, amount, now));
        telemetry.paymentRefunded(saved, amount);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment get(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentPage findByMerchant(UUID merchantId, int page, int size) {
        return paymentRepository.findByMerchantId(merchantId, page, size);
    }

    private String fingerprint(CreatePaymentCommand command) {
        String canonical = encode(command.merchantId())
                + encode(command.orderId())
                + encode(command.amount().amount().toPlainString())
                + encode(command.amount().currency())
                + encode(command.paymentToken())
                + encode(command.description());
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String encode(Object value) {
        String text = String.valueOf(value);
        return text.length() + ":" + text;
    }

    private void completeIdempotencyAfterCommit(
            String key,
            String fingerprint,
            String leaseToken,
            UUID paymentId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            idempotency.complete(key, fingerprint, leaseToken, paymentId, IDEMPOTENCY_TTL);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    idempotency.complete(
                            key,
                            fingerprint,
                            leaseToken,
                            paymentId,
                            IDEMPOTENCY_TTL);
                } catch (RuntimeException exception) {
                    LOGGER.error(
                            "idempotency_completion_failed paymentId={} key={}",
                            paymentId,
                            key,
                            exception);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    idempotency.release(key, fingerprint, leaseToken);
                }
            }
        });
    }
}
