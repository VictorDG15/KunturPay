package pe.victoryordi.paycore.application.port.in.command;

import pe.victoryordi.paycore.domain.model.Money;

import java.util.UUID;

public record CreatePaymentCommand(
        UUID merchantId,
        String orderId,
        Money amount,
        String paymentToken,
        String description) {
}
