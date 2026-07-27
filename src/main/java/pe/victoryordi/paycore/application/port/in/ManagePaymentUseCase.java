package pe.victoryordi.paycore.application.port.in;

import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

import java.util.UUID;

public interface ManagePaymentUseCase {

    Payment capture(UUID paymentId);

    Payment refund(UUID paymentId, Money amount);
}
