package pe.victoryordi.paycore.application.port.out;

import pe.victoryordi.paycore.domain.model.Money;
import pe.victoryordi.paycore.domain.model.Payment;

public interface PaymentGatewayPort {

    AuthorizationResult authorize(Payment payment);

    void capture(Payment payment);

    void refund(Payment payment, Money amount);

    record AuthorizationResult(boolean approved, String authorizationCode, String declineReason) {

        public static AuthorizationResult approved(String code) {
            return new AuthorizationResult(true, code, null);
        }

        public static AuthorizationResult declined(String reason) {
            return new AuthorizationResult(false, null, reason);
        }
    }
}
