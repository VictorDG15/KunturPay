package pe.victoryordi.paycore.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.victoryordi.paycore.application.port.in.command.CreatePaymentCommand;
import pe.victoryordi.paycore.domain.model.Currency;
import pe.victoryordi.paycore.domain.model.Money;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID merchantId,
        @NotBlank @Size(max = 80) String orderId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull Currency currency,
        @NotBlank
        @Size(max = 120)
        @Pattern(
                regexp = "^tok_[A-Za-z0-9_-]{6,}$",
                message = "must be a token; raw card data is not accepted")
        String paymentToken,
        @Size(max = 200) String description) {

    public CreatePaymentCommand toCommand() {
        return new CreatePaymentCommand(
                merchantId,
                orderId,
                new Money(amount, currency),
                paymentToken,
                description);
    }
}
