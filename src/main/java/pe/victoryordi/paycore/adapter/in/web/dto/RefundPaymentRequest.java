package pe.victoryordi.paycore.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import pe.victoryordi.paycore.domain.model.Currency;
import pe.victoryordi.paycore.domain.model.Money;

import java.math.BigDecimal;

public record RefundPaymentRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull Currency currency) {

    public Money toMoney() {
        return new Money(amount, currency);
    }
}
