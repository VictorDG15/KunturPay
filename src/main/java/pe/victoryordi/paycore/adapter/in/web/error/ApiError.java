package pe.victoryordi.paycore.adapter.in.web.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        boolean success,
        String code,
        String message,
        int status,
        String path,
        String correlationId,
        Instant timestamp,
        Map<String, String> fieldErrors) {

    public static ApiError of(
            String code,
            String message,
            int status,
            String path,
            String correlationId,
            Map<String, String> fieldErrors) {
        return new ApiError(
                false,
                code,
                message,
                status,
                path,
                correlationId,
                Instant.now(),
                Map.copyOf(fieldErrors));
    }
}
