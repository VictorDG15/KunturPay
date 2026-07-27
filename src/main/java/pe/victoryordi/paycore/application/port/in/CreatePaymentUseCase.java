package pe.victoryordi.paycore.application.port.in;

import pe.victoryordi.paycore.application.port.in.command.CreatePaymentCommand;
import pe.victoryordi.paycore.domain.model.Payment;

public interface CreatePaymentUseCase {

    Payment create(CreatePaymentCommand command, String idempotencyKey);
}
