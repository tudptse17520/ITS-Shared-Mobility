package vn.edu.ut.its.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import vn.edu.ut.its.dto.request.ManualStatusRequestDTO;
import vn.edu.ut.its.dto.request.PaymentRequestDTO;
import vn.edu.ut.its.dto.response.PaymentListResponseDTO;
import vn.edu.ut.its.dto.response.PaymentResponseDTO;
import vn.edu.ut.its.service.PaymentService;


@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Payment")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment transaction.
     * Creates booking deposits and ride fare payments using cash or VNPay.
     * Cash payments are automatically set to SUCCESS.
     *
     * @param requestDTO the payment data (validated with @Valid)
     * @return HTTP 201 Created with the created payment
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @Valid @RequestBody PaymentRequestDTO requestDTO,
            HttpServletRequest request) {
        PaymentResponseDTO createdPayment = paymentService.createPayment(requestDTO, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
    }

    /**
     * Get the remaining debt details for a ride session.
     * This provides total fee, paid fee (including booking deposits), and remaining fee.
     *
     * @param sessionId the ride session ID
     * @return the debt details
     */
    @GetMapping("/ride-sessions/{sessionId}/debt")
    public ResponseEntity<vn.edu.ut.its.dto.response.PaymentDebtResponseDTO> getRemainingDebt(
            @PathVariable("sessionId") Long sessionId) {
        vn.edu.ut.its.dto.response.PaymentDebtResponseDTO response = paymentService.getRemainingDebt(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve a filtered list of payment transactions.
     * All query parameters are optional.
     *
     * @param paymentMethod filter by payment method (Cash, Momo, Vnpay, Credit_Card)
     * @param status        filter by payment status (PENDING, SUCCESS, FAILED)
     * @param fromDate      filter from date (format DD-MM-YYYY)
     * @return HTTP 200 OK with wrapped payment list
     */
    @GetMapping
    public ResponseEntity<PaymentListResponseDTO> getPayments(
            @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "fromDate", required = false) String fromDate) {
        PaymentListResponseDTO response = paymentService.getPayments(paymentMethod, status, fromDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve detail of a specific payment receipt.
     *
     * @param id the payment ID (path variable)
     * @return HTTP 200 OK with payment detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(@PathVariable Long id) {
        PaymentResponseDTO response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * An administrator can update a PENDING payment.
     * Requires status (SUCCESS/FAILED) and reason (min 10 chars).
     *
     * @param id         the payment ID (path variable)
     * @param requestDTO the new status and reason
     * @return HTTP 200 OK with updated payment
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDTO> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ManualStatusRequestDTO requestDTO) {
        PaymentResponseDTO updatedPayment = paymentService.updatePaymentStatus(id, requestDTO);
        return ResponseEntity.ok(updatedPayment);
    }

    /**
     * Cancel a PENDING payment transaction.
     *
     * @param id the payment ID (path variable)
     * @return HTTP 200 OK with cancelled payment
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponseDTO> cancelPayment(@PathVariable Long id) {
        PaymentResponseDTO cancelledPayment = paymentService.cancelPayment(id);
        return ResponseEntity.ok(cancelledPayment);
    }

    /**
     * Webhook endpoint for VNPAY Instant Payment Notification (IPN).
     *
     * @param request the HTTP request containing VNPAY parameters
     * @return HTTP 200 OK with VNPAY formatted JSON response
     */
    @GetMapping("/vnpay-ipn")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> processVnpayIpn(HttpServletRequest request) {
        return paymentService.processVnpayIpn(request);
    }

    @GetMapping("/vnpay-return")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> processVnpayReturn(HttpServletRequest request) {
        return paymentService.processVnpayReturn(request);
    }
}
