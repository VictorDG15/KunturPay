package pe.victoryordi.paycore.adapter.in.web.dto;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, Meta meta) {

    public static <T> ApiResponse<T> of(T data, String correlationId) {
        return new ApiResponse<>(true, data, new Meta(Instant.now(), correlationId));
    }

    public record Meta(Instant timestamp, String correlationId) {
    }
}
