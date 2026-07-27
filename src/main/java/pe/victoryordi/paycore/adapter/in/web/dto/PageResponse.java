package pe.victoryordi.paycore.adapter.in.web.dto;

import pe.victoryordi.paycore.application.port.out.PaymentPage;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static PageResponse<PaymentResponse> from(PaymentPage source) {
        return new PageResponse<>(
                source.content().stream().map(PaymentResponse::from).toList(),
                source.page(),
                source.size(),
                source.totalElements(),
                source.totalPages());
    }
}
