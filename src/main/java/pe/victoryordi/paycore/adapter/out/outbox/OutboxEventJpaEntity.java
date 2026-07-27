package pe.victoryordi.paycore.adapter.out.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
class OutboxEventJpaEntity {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "aggregate_type", nullable = false, length = 60)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    protected OutboxEventJpaEntity() {
    }

    OutboxEventJpaEntity(
            UUID id,
            String eventType,
            int eventVersion,
            String aggregateType,
            UUID aggregateId,
            JsonNode payload,
            Instant occurredAt) {
        this.id = id;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.nextAttemptAt = occurredAt;
    }

    void markPublished(Instant now) {
        publishedAt = now;
        lastError = null;
    }

    void markFailed(String error, Instant retryAt) {
        attempts++;
        lastError = error == null ? "unknown" : error.substring(0, Math.min(error.length(), 500));
        nextAttemptAt = retryAt;
    }

    UUID getId() {
        return id;
    }

    String getEventType() {
        return eventType;
    }

    UUID getAggregateId() {
        return aggregateId;
    }

    JsonNode getPayload() {
        return payload;
    }

    int getAttempts() {
        return attempts;
    }
}
