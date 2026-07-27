package pe.victoryordi.paycore.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import pe.victoryordi.paycore.adapter.out.persistence.entity.PaymentJpaEntity;
import pe.victoryordi.paycore.domain.model.Currency;
import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;
import pe.victoryordi.paycore.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentPersistenceAdapterTest {

    @Mock
    private PaymentJpaRepository repository;

    @Test
    void shouldMapTheDomainWithoutLosingFinancialState() {
        PaymentPersistenceAdapter adapter = new PaymentPersistenceAdapter(repository);
        Payment payment = authorizedPayment();
        PaymentJpaEntity persisted = new PaymentJpaEntity(
                10L,
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
                0);
        when(repository.saveAndFlush(any(PaymentJpaEntity.class))).thenReturn(persisted);

        Payment saved = adapter.save(payment);

        ArgumentCaptor<PaymentJpaEntity> entity = ArgumentCaptor.forClass(PaymentJpaEntity.class);
        verify(repository).saveAndFlush(entity.capture());
        assertThat(entity.getValue().getAmount()).isEqualByComparingTo("150.00");
        assertThat(saved.id()).isEqualTo(payment.id());
        assertThat(saved.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(saved.authorizationCode()).isEqualTo("AUTH-123");
    }

    @Test
    void shouldMapQueriesAndPagination() {
        PaymentPersistenceAdapter adapter = new PaymentPersistenceAdapter(repository);
        PaymentJpaEntity entity = entity();
        UUID paymentId = entity.getPublicId();
        UUID merchantId = entity.getMerchantId();
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(repository.findByPublicId(paymentId)).thenReturn(Optional.of(entity));
        when(repository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageRequest))
                .thenReturn(new PageImpl<>(List.of(entity), pageRequest, 1));

        assertThat(adapter.findById(paymentId)).isPresent();
        var page = adapter.findByMerchantId(merchantId, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    private Payment authorizedPayment() {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                "ORDER-1001",
                new Money(new BigDecimal("150.00"), Currency.PEN),
                "tok_test_visa_4242",
                "Compra",
                Instant.parse("2026-07-26T00:00:00Z"));
        payment.authorize("AUTH-123", Instant.parse("2026-07-26T00:00:01Z"));
        return payment;
    }

    private PaymentJpaEntity entity() {
        return new PaymentJpaEntity(
                10L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ORDER-1001",
                new BigDecimal("150.00"),
                Currency.PEN,
                "tok_test_visa_4242",
                "Compra",
                PaymentStatus.AUTHORIZED,
                "AUTH-123",
                null,
                BigDecimal.ZERO,
                Instant.parse("2026-07-26T00:00:00Z"),
                Instant.parse("2026-07-26T00:00:01Z"),
                2);
    }
}
