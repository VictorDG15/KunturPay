package pe.victoryordi.paycore.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import pe.victoryordi.paycore.domain.model.Currency;
import pe.victoryordi.paycore.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "order_id", nullable = false, length = 80)
    private String orderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "payment_token", nullable = false, length = 120)
    private String paymentToken;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "authorization_code", length = 80)
    private String authorizationCode;

    @Column(name = "decline_reason", length = 160)
    private String declineReason;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected PaymentJpaEntity() {
    }

    public PaymentJpaEntity(
            Long id,
            UUID publicId,
            UUID merchantId,
            String orderId,
            BigDecimal amount,
            Currency currency,
            String paymentToken,
            String description,
            PaymentStatus status,
            String authorizationCode,
            String declineReason,
            BigDecimal refundedAmount,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = id;
        this.publicId = publicId;
        this.merchantId = merchantId;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.paymentToken = paymentToken;
        this.description = description;
        this.status = status;
        this.authorizationCode = authorizationCode;
        this.declineReason = declineReason;
        this.refundedAmount = refundedAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getPaymentToken() {
        return paymentToken;
    }

    public String getDescription() {
        return description;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
