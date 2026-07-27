package pe.victoryordi.paycore.domain.model;

import org.junit.jupiter.api.Test;
import pe.victoryordi.paycore.domain.exception.InvalidPaymentStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

    @Test
    void shouldFollowAuthorizationCaptureAndFullRefundFlow() {
        Payment payment = newPayment("tok_test_4242");

        payment.authorize("AUTH-123", NOW.plusSeconds(1));
        payment.capture(NOW.plusSeconds(2));
        payment.refund(new Money(new BigDecimal("150.00"), Currency.PEN), NOW.plusSeconds(3));

        assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.refundedAmount().amount()).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldSupportPartialRefunds() {
        Payment payment = newPayment("tok_test_4242");
        payment.authorize("AUTH-123", NOW);
        payment.capture(NOW);

        payment.refund(new Money(new BigDecimal("50.00"), Currency.PEN), NOW);

        assertThat(payment.status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(payment.refundedAmount().amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldRejectCaptureOfDeclinedPayment() {
        Payment payment = newPayment("tok_test_0000");
        payment.decline("insufficient_funds", NOW);

        assertThatThrownBy(() -> payment.capture(NOW))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("DECLINED");
    }

    @Test
    void shouldRejectRefundGreaterThanCapturedAmount() {
        Payment payment = newPayment("tok_test_4242");
        payment.authorize("AUTH-123", NOW);
        payment.capture(NOW);

        assertThatThrownBy(() ->
                payment.refund(new Money(new BigDecimal("150.01"), Currency.PEN), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }

    private Payment newPayment(String token) {
        return Payment.create(
                UUID.randomUUID(),
                "ORDER-001",
                new Money(new BigDecimal("150.00"), Currency.PEN),
                token,
                "Compra de prueba",
                NOW);
    }
}
