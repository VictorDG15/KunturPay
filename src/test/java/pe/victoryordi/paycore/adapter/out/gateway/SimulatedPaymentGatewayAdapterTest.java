package pe.victoryordi.paycore.adapter.out.gateway;

import org.junit.jupiter.api.Test;
import pe.victoryordi.paycore.domain.model.Currency;
import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayAdapterTest {

    private final SimulatedPaymentGatewayAdapter gateway = new SimulatedPaymentGatewayAdapter();

    @Test
    void shouldApproveARegularToken() {
        var result = gateway.authorize(payment("tok_test_visa_4242"));

        assertThat(result.approved()).isTrue();
        assertThat(result.authorizationCode()).startsWith("AUTH-");
    }

    @Test
    void shouldDeclineTheDocumentedTestTokens() {
        var result = gateway.authorize(payment("tok_test_visa_0000"));

        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).isEqualTo("insufficient_funds");
    }

    private Payment payment(String token) {
        return Payment.create(
                UUID.randomUUID(),
                "ORDER-1001",
                new Money(new BigDecimal("20.00"), Currency.PEN),
                token,
                null,
                Instant.parse("2026-07-26T00:00:00Z"));
    }
}
