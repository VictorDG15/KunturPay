package pe.victoryordi.paycore.application.port.in;

import pe.victoryordi.paycore.application.port.out.PaymentPage;
import pe.victoryordi.paycore.domain.model.Payment;

import java.util.UUID;

public interface QueryPaymentUseCase {

    Payment get(UUID paymentId);

    PaymentPage findByMerchant(UUID merchantId, int page, int size);
}
