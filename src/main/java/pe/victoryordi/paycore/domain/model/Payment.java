package pe.victoryordi.paycore.domain.model;

import pe.victoryordi.paycore.domain.exception.InvalidPaymentStateException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Payment {

    private final Long internalId;
    private final UUID id;
    private final UUID merchantId;
    private final String orderId;
    private final Money amount;
    private final String paymentToken;
    private final String description;
    private PaymentStatus status;
    private String authorizationCode;
    private String declineReason;
    private Money refundedAmount;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    private Payment(
            Long internalId,
            UUID id,
            UUID merchantId,
            String orderId,
            Money amount,
            String paymentToken,
            String description,
            PaymentStatus status,
            String authorizationCode,
            String declineReason,
            Money refundedAmount,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.internalId = internalId;
        this.id = Objects.requireNonNull(id);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.orderId = requireText(orderId, "orderId");
        this.amount = Objects.requireNonNull(amount);
        this.paymentToken = requireText(paymentToken, "paymentToken");
        this.description = description;
        this.status = Objects.requireNonNull(status);
        this.authorizationCode = authorizationCode;
        this.declineReason = declineReason;
        this.refundedAmount = Objects.requireNonNull(refundedAmount);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.version = version;
    }

    public static Payment create(
            UUID merchantId,
            String orderId,
            Money amount,
            String paymentToken,
            String description,
            Instant now) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("payment amount must be greater than zero");
        }
        return new Payment(
                null,
                UUID.randomUUID(),
                merchantId,
                orderId,
                amount,
                paymentToken,
                description,
                PaymentStatus.PENDING,
                null,
                null,
                Money.zero(amount.currency()),
                now,
                now,
                0);
    }

    public static Payment rehydrate(
            Long internalId,
            UUID id,
            UUID merchantId,
            String orderId,
            Money amount,
            String paymentToken,
            String description,
            PaymentStatus status,
            String authorizationCode,
            String declineReason,
            Money refundedAmount,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        return new Payment(
                internalId, id, merchantId, orderId, amount, paymentToken, description, status,
                authorizationCode, declineReason, refundedAmount, createdAt, updatedAt, version);
    }

    public void authorize(String code, Instant now) {
        requireStatus(PaymentStatus.PENDING);
        authorizationCode = requireText(code, "authorizationCode");
        status = PaymentStatus.AUTHORIZED;
        touch(now);
    }

    public void decline(String reason, Instant now) {
        requireStatus(PaymentStatus.PENDING);
        declineReason = requireText(reason, "declineReason");
        status = PaymentStatus.DECLINED;
        touch(now);
    }

    public void capture(Instant now) {
        requireStatus(PaymentStatus.AUTHORIZED);
        status = PaymentStatus.CAPTURED;
        touch(now);
    }

    public void refund(Money refund, Instant now) {
        if (status != PaymentStatus.CAPTURED && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidPaymentStateException(id, status, "refund");
        }
        if (refund.isZero()) {
            throw new IllegalArgumentException("refund amount must be greater than zero");
        }
        Money accumulated = refundedAmount.add(refund);
        if (accumulated.isGreaterThan(amount)) {
            throw new IllegalArgumentException("refund amount exceeds captured amount");
        }
        refundedAmount = accumulated;
        status = accumulated.amount().compareTo(amount.amount()) == 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        touch(now);
    }

    private void requireStatus(PaymentStatus expected) {
        if (status != expected) {
            throw new InvalidPaymentStateException(id, status, "transition to next state");
        }
    }

    private void touch(Instant now) {
        updatedAt = Objects.requireNonNull(now);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    public UUID id() {
        return id;
    }

    public Long internalId() {
        return internalId;
    }

    public UUID merchantId() {
        return merchantId;
    }

    public String orderId() {
        return orderId;
    }

    public Money amount() {
        return amount;
    }

    public String paymentToken() {
        return paymentToken;
    }

    public String description() {
        return description;
    }

    public PaymentStatus status() {
        return status;
    }

    public String authorizationCode() {
        return authorizationCode;
    }

    public String declineReason() {
        return declineReason;
    }

    public Money refundedAmount() {
        return refundedAmount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }
}
