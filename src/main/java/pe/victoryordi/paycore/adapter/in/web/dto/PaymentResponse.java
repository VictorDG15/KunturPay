package pe.victoryordi.paycore.adapter.in.web.dto;

import pe.victoryordi.paycore.domain.model.Currency;
import pe.victoryordi.paycore.domain.model.Payment;
import pe.victoryordi.paycore.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID merchantId,
        String orderId,
        BigDecimal amount,
        Currency currency,
        PaymentStatus status,
        String tokenReference,
        String description,
        String authorizationCode,
        String declineReason,
        BigDecimal refundedAmount,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.merchantId(),
                payment.orderId(),
                payment.amount().amount(),
                payment.amount().currency(),
                payment.status(),
                mask(payment.paymentToken()),
                payment.description(),
                payment.authorizationCode(),
                payment.declineReason(),
                payment.refundedAmount().amount(),
                payment.createdAt(),
                payment.updatedAt());
    }

    private static String mask(String token) {
        int visible = Math.min(4, token.length());
        return "****" + token.substring(token.length() - visible);
    }
}
