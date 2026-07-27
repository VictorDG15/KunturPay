package pe.victoryordi.paycore.adapter.in.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.victoryordi.paycore.domain.exception.IdempotencyConflictException;
import pe.victoryordi.paycore.domain.exception.InvalidPaymentStateException;
import pe.victoryordi.paycore.domain.exception.PaymentNotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(
            PaymentNotFoundException exception,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    ResponseEntity<ApiError> handleInvalidState(
            InvalidPaymentStateException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_PAYMENT_STATE",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ApiError> handleIdempotency(
            IdempotencyConflictException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_CONFLICT",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiError> handleConflict(Exception exception, HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "CONCURRENT_UPDATE",
                "The resource was modified or already exists",
                request,
                Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "The request contains invalid fields",
                request,
                fields);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> handleBadRequest(Exception exception, HttpServletRequest request) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("unexpected_error correlationId={}", MDC.get("correlationId"), exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request,
                Map.of());
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> fields) {
        return ResponseEntity.status(status).body(ApiError.of(
                code,
                message,
                status.value(),
                request.getRequestURI(),
                MDC.get("correlationId"),
                fields));
    }
}
