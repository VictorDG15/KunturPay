package pe.victoryordi.paycore.application.port.out;

import pe.victoryordi.paycore.domain.model.Payment;

import java.util.List;

public record PaymentPage(
        List<Payment> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public PaymentPage {
        content = List.copyOf(content);
    }
}
