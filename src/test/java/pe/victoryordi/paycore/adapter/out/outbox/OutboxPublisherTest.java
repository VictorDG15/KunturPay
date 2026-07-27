package pe.victoryordi.paycore.adapter.out.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import pe.victoryordi.paycore.domain.event.DomainEvent;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

    @Mock
    private OutboxJpaRepository repository;
    @Mock
    private KafkaTemplate<String, String> kafka;
    @Mock
    private Environment environment;

    @Test
    void shouldPublishPendingEventsUsingTheAggregateAsKafkaKey() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEventJpaEntity event = event(aggregateId);
        when(environment.getProperty(
                "app.kafka.payment-events-topic",
                "kunturpay.payments.v1"))
                .thenReturn("payments.test.v1");
        when(repository.findPending(eq(NOW), any(Pageable.class))).thenReturn(List.of(event));
        when(kafka.send(eq("payments.test.v1"), eq(aggregateId.toString()), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));
        OutboxPublisher publisher = new OutboxPublisher(
                repository,
                kafka,
                Clock.fixed(NOW, ZoneOffset.UTC),
                environment);

        publisher.publishBatch();

        verify(kafka).send(
                eq("payments.test.v1"),
                eq(aggregateId.toString()),
                any(String.class));
        assertThat(event.getAttempts()).isZero();
    }

    @Test
    void shouldBackoffWithoutLosingAnEventWhenKafkaFails() {
        OutboxEventJpaEntity event = event(UUID.randomUUID());
        when(repository.findPending(eq(NOW), any(Pageable.class))).thenReturn(List.of(event));
        when(kafka.send(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        OutboxPublisher publisher = new OutboxPublisher(
                repository,
                kafka,
                Clock.fixed(NOW, ZoneOffset.UTC),
                environment);

        publisher.publishBatch();

        assertThat(event.getAttempts()).isEqualTo(1);
    }

    @Test
    void shouldPersistTheCompleteEventEnvelopeInTheOutbox() {
        JpaOutboxAdapter adapter = new JpaOutboxAdapter(repository, new ObjectMapper().findAndRegisterModules());
        DomainEvent domainEvent = DomainEvent.payment(
                "payment.authorized.v1",
                UUID.randomUUID(),
                NOW,
                Map.of("status", "AUTHORIZED"));

        adapter.append(domainEvent);

        ArgumentCaptor<OutboxEventJpaEntity> captor =
                ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(domainEvent.eventId());
        assertThat(captor.getValue().getPayload().get("eventType").asText())
                .isEqualTo("payment.authorized.v1");
    }

    private OutboxEventJpaEntity event(UUID aggregateId) {
        return new OutboxEventJpaEntity(
                UUID.randomUUID(),
                "payment.authorized.v1",
                1,
                "payment",
                aggregateId,
                new ObjectMapper().valueToTree(Map.of("status", "AUTHORIZED")),
                NOW);
    }
}
