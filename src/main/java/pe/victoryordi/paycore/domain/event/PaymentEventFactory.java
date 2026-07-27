package pe.victoryordi.paycore.domain.event;

import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;
import pe.victoryordi.paycore.domain.model.PaymentStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PaymentEventFactory {

    private PaymentEventFactory() {
    }

    public static DomainEvent authorizationCompleted(Payment payment, Instant now) {
        String type = payment.status() == PaymentStatus.AUTHORIZED
                ? "payment.authorized.v1"
                : "payment.declined.v1";
        return event(type, payment, now, payment.amount());
    }

    public static DomainEvent captured(Payment payment, Instant now) {
        return event("payment.captured.v1", payment, now, payment.amount());
    }

    public static DomainEvent refunded(Payment payment, Money refund, Instant now) {
        return event("payment.refunded.v1", payment, now, refund);
    }

    private static DomainEvent event(String type, Payment payment, Instant now, Money operationAmount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.id());
        payload.put("merchantId", payment.merchantId());
        payload.put("orderId", payment.orderId());
        payload.put("amount", operationAmount.amount());
        payload.put("currency", operationAmount.currency());
        payload.put("status", payment.status());
        return DomainEvent.payment(type, payment.id(), now, payload);
    }
}
