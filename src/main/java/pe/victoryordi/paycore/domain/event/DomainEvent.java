package pe.victoryordi.paycore.domain.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        Map<String, Object> payload) {

    public static DomainEvent payment(
            String eventType,
            UUID paymentId,
            Instant occurredAt,
            Map<String, Object> payload) {
        return new DomainEvent(
                UUID.randomUUID(),
                eventType,
                1,
                "payment",
                paymentId,
                occurredAt,
                Map.copyOf(payload));
    }
}
