package pe.victoryordi.paycore.application.port.out;

import pe.victoryordi.paycore.domain.event.DomainEvent;

public interface OutboxPort {

    void append(DomainEvent event);
}
