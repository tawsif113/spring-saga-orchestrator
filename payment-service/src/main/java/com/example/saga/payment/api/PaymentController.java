package com.example.saga.payment.api;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.PaymentId;
import com.example.saga.common.Money;
import com.example.saga.payment.app.PaymentCommandService;
import com.example.saga.payment.domain.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentCommandService service;

  public PaymentController(PaymentCommandService service) {
    this.service = service;
  }

  public record CreatePaymentRequest(@NotNull UUID orderId, @NotNull BigDecimal amount, @NotBlank String currency) {
  }

  public record AuthorizeRequest(@NotBlank String authCode) {
  }

  public record RejectRequest(@NotBlank String reason) {
  }

  public record PaymentResponse(
      UUID paymentId,
      UUID orderId,
      BigDecimal amount,
      String currency,
      String status,
      String authCode,
      String rejectionReason
  ) {
  }

  @PostMapping
  public PaymentResponse create(@RequestBody CreatePaymentRequest req) {
    Payment payment = service.create(
        new OrderId(req.orderId()),
        new Money(req.amount(), req.currency())
    );
    return toResponse(payment);
  }

  @PostMapping("/{paymentId}/authorize")
  public PaymentResponse authorize(@PathVariable UUID paymentId, @RequestBody AuthorizeRequest req) {
    return toResponse(service.authorize(new PaymentId(paymentId), req.authCode()));
  }

  @PostMapping("/{paymentId}/reject")
  public PaymentResponse reject(@PathVariable UUID paymentId, @RequestBody RejectRequest req) {
    return toResponse(service.reject(new PaymentId(paymentId), req.reason()));
  }

  @GetMapping("/{paymentId}")
  public PaymentResponse get(@PathVariable UUID paymentId) {
    return toResponse(service.get(new PaymentId(paymentId)));
  }

  @GetMapping("/by-order/{orderId}")
  public PaymentResponse getByOrderId(@PathVariable UUID orderId) {
    return toResponse(service.getByOrderId(new OrderId(orderId)));
  }

  private static PaymentResponse toResponse(Payment payment) {
    return new PaymentResponse(
        payment.id().value(),
        payment.orderId().value(),
        payment.amount().amount(),
        payment.amount().currency(),
        payment.status().name(),
        payment.authCode(),
        payment.rejectionReason()
    );
  }
}
