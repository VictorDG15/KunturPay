package pe.victoryordi.paycore.adapter.out.gateway;

import org.springframework.stereotype.Component;
import pe.victoryordi.paycore.application.port.out.PaymentGatewayPort;
import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

import java.util.Locale;
import java.util.UUID;

@Component
public class SimulatedPaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public AuthorizationResult authorize(Payment payment) {
        String normalizedToken = payment.paymentToken().toLowerCase(Locale.ROOT);
        if (normalizedToken.endsWith("0000") || normalizedToken.contains("declined")) {
            return AuthorizationResult.declined("insufficient_funds");
        }
        return AuthorizationResult.approved("AUTH-" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Override
    public void capture(Payment payment) {
        // Punto de extensión para integrar un adquirente real.
    }

    @Override
    public void refund(Payment payment, Money amount) {
        // Punto de extensión para integrar un adquirente real.
    }
}
