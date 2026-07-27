package pe.victoryordi.paycore.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import pe.victoryordi.paycore.adapter.out.persistence.entity.PaymentJpaEntity;
import pe.victoryordi.paycore.application.port.out.PaymentPage;
import pe.victoryordi.paycore.application.port.out.PaymentRepositoryPort;
import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentPersistenceAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository repository;

    public PaymentPersistenceAdapter(PaymentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Payment save(Payment payment) {
        return toDomain(repository.saveAndFlush(toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return repository.findByPublicId(paymentId).map(PaymentPersistenceAdapter::toDomain);
    }

    @Override
    public PaymentPage findByMerchantId(UUID merchantId, int page, int size) {
        Page<PaymentJpaEntity> result =
                repository.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, size));
        return new PaymentPage(
                result.getContent().stream().map(PaymentPersistenceAdapter::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private static PaymentJpaEntity toEntity(Payment payment) {
        return new PaymentJpaEntity(
                payment.internalId(),
                payment.id(),
                payment.merchantId(),
                payment.orderId(),
                payment.amount().amount(),
                payment.amount().currency(),
                payment.paymentToken(),
                payment.description(),
                payment.status(),
                payment.authorizationCode(),
                payment.declineReason(),
                payment.refundedAmount().amount(),
                payment.createdAt(),
                payment.updatedAt(),
                payment.version());
    }

    private static Payment toDomain(PaymentJpaEntity entity) {
        return Payment.rehydrate(
                entity.getId(),
                entity.getPublicId(),
                entity.getMerchantId(),
                entity.getOrderId(),
                new Money(entity.getAmount(), entity.getCurrency()),
                entity.getPaymentToken(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getAuthorizationCode(),
                entity.getDeclineReason(),
                new Money(entity.getRefundedAmount(), entity.getCurrency()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }
}
