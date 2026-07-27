package pe.victoryordi.paycore.adapter.in.web.dto;

import org.junit.jupiter.api.Test;
import pe.victoryordi.paycore.application.port.out.PaymentPage;
import pe.victoryordi.paycore.domain.model.Currency;
import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDtoTest {

    @Test
    void shouldMapRequestToCommandAndNeverExposeTheFullToken() {
        UUID merchantId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(
                merchantId,
                "ORDER-1001",
                new BigDecimal("150.00"),
                Currency.PEN,
                "tok_test_visa_4242",
                "Compra");

        var command = request.toCommand();
        Payment payment = Payment.create(
                command.merchantId(),
                command.orderId(),
                command.amount(),
                command.paymentToken(),
                command.description(),
                Instant.parse("2026-07-26T00:00:00Z"));

        PaymentResponse response = PaymentResponse.from(payment);

        assertThat(command.amount()).isEqualTo(new Money(new BigDecimal("150.00"), Currency.PEN));
        assertThat(response.merchantId()).isEqualTo(merchantId);
        assertThat(response.tokenReference()).isEqualTo("****4242");
        assertThat(response.toString()).doesNotContain("tok_test_visa_4242");
    }

    @Test
    void shouldMapAStablePageContract() {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                "ORDER-1001",
                new Money(new BigDecimal("20.00"), Currency.USD),
                "tok_short",
                null,
                Instant.parse("2026-07-26T00:00:00Z"));
        PaymentPage source = new PaymentPage(List.of(payment), 1, 10, 21, 3);

        PageResponse<PaymentResponse> response = PageResponse.from(source);

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(21);
        assertThat(response.totalPages()).isEqualTo(3);
    }
}
