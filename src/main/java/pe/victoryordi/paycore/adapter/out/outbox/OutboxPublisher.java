package pe.victoryordi.paycore.adapter.out.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(
        name = "app.outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;
    private final String topic;

    public OutboxPublisher(
            OutboxJpaRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            Clock clock,
            org.springframework.core.env.Environment environment) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.topic = environment.getProperty(
                "app.kafka.payment-events-topic",
                "kunturpay.payments.v1");
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.delay-ms:1000}")
    @Transactional
    public void publishBatch() {
        Instant now = clock.instant();
        List<OutboxEventJpaEntity> events =
                repository.findPending(now, PageRequest.of(0, BATCH_SIZE));

        for (OutboxEventJpaEntity event : events) {
            try {
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload().toString())
                        .get(5, TimeUnit.SECONDS);
                event.markPublished(now);
                LOGGER.info("outbox_event_published eventId={} eventType={}",
                        event.getId(), event.getEventType());
            } catch (Exception exception) {
                Duration backoff = Duration.ofSeconds(Math.min(300, 1L << Math.min(event.getAttempts(), 8)));
                event.markFailed(exception.getMessage(), now.plus(backoff));
                LOGGER.warn("outbox_event_failed eventId={} retryInSeconds={}",
                        event.getId(), backoff.toSeconds());
            }
        }
    }
}
