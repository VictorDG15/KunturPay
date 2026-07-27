package pe.victoryordi.paycore.application.port.out;

import pe.victoryordi.paycore.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID paymentId);

    PaymentPage findByMerchantId(UUID merchantId, int page, int size);
}
