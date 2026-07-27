package pe.victoryordi.paycore.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.victoryordi.paycore.adapter.in.web.dto.ApiResponse;
import pe.victoryordi.paycore.adapter.in.web.dto.CreatePaymentRequest;
import pe.victoryordi.paycore.adapter.in.web.dto.PageResponse;
import pe.victoryordi.paycore.adapter.in.web.dto.PaymentResponse;
import pe.victoryordi.paycore.adapter.in.web.dto.RefundPaymentRequest;
import pe.victoryordi.paycore.application.port.in.CreatePaymentUseCase;
import pe.victoryordi.paycore.application.port.in.ManagePaymentUseCase;
import pe.victoryordi.paycore.application.port.in.QueryPaymentUseCase;
import pe.victoryordi.paycore.domain.model.Payment;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreatePaymentUseCase createPayment;
    private final ManagePaymentUseCase managePayment;
    private final QueryPaymentUseCase queryPayment;

    public PaymentController(
            CreatePaymentUseCase createPayment,
            ManagePaymentUseCase managePayment,
            QueryPaymentUseCase queryPayment) {
        this.createPayment = createPayment;
        this.managePayment = managePayment;
        this.queryPayment = queryPayment;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> create(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 255) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = createPayment.create(request.toCommand(), idempotencyKey);
        return ResponseEntity
                .created(URI.create("/api/v1/payments/" + payment.id()))
                .body(ApiResponse.of(PaymentResponse.from(payment), correlationId()));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> get(@PathVariable UUID paymentId) {
        return ApiResponse.of(PaymentResponse.from(queryPayment.get(paymentId)), correlationId());
    }

    @GetMapping
    public ApiResponse<PageResponse<PaymentResponse>> findByMerchant(
            @RequestParam UUID merchantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(
                PageResponse.from(queryPayment.findByMerchant(merchantId, page, size)),
                correlationId());
    }

    @PostMapping("/{paymentId}/capture")
    public ApiResponse<PaymentResponse> capture(@PathVariable UUID paymentId) {
        return ApiResponse.of(
                PaymentResponse.from(managePayment.capture(paymentId)),
                correlationId());
    }

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundPaymentRequest request) {
        Payment payment = managePayment.refund(paymentId, request.toMoney());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/v1/payments/" + payment.id())
                .body(ApiResponse.of(PaymentResponse.from(payment), correlationId()));
    }

    private String correlationId() {
        return MDC.get("correlationId");
    }
}
