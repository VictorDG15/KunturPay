package pe.victoryordi.paycore.adapter.out.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import pe.victoryordi.paycore.application.port.out.OutboxPort;
import pe.victoryordi.paycore.domain.event.DomainEvent;

@Component
public class JpaOutboxAdapter implements OutboxPort {

    private final OutboxJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaOutboxAdapter(OutboxJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(DomainEvent event) {
        repository.save(new OutboxEventJpaEntity(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.aggregateType(),
                event.aggregateId(),
                objectMapper.valueToTree(event),
                event.occurredAt()));
    }
}
