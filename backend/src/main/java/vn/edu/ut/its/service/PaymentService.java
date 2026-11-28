package vn.edu.ut.its.service;

import vn.edu.ut.its.dto.request.ManualStatusRequestDTO;
import vn.edu.ut.its.dto.request.PaymentRequestDTO;
import vn.edu.ut.its.dto.response.PaymentListResponseDTO;
import vn.edu.ut.its.dto.response.PaymentResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface PaymentService {

    /**
     * Create a new payment transaction.
     * Validates the amount against the booking deposit or outstanding ride fare.
     * Cash payments are automatically set to SUCCESS.
     *
     * @param requestDTO the payment data from the client
     * @param request the HTTP request for VNPay payment details
     * @return the created payment as a response DTO
     */
    PaymentResponseDTO createPayment(PaymentRequestDTO requestDTO, HttpServletRequest request);

    /**
     * Retrieve a filtered list of payment transactions.
     *
     * @param paymentMethod filter by payment method (nullable)
     * @param status        filter by payment status (nullable)
     * @param fromDate      filter from date in DD-MM-YYYY format (nullable)
     * @return wrapped response with total_items, message, and data list
     */
    PaymentListResponseDTO getPayments(String paymentMethod, String status, String fromDate);

    /**
     * Retrieve detail of a specific payment by ID.
     *
     * @param id the payment ID
     * @return the payment detail as a response DTO
     */
    PaymentResponseDTO getPaymentById(Long id);

    /**
     * Calculate and return the remaining debt for a ride session.
     *
     * @param sessionId the ride session ID
     * @return the debt details including total fee, paid fee, and remaining fee
     */
    vn.edu.ut.its.dto.response.PaymentDebtResponseDTO getRemainingDebt(Long sessionId);

    /**
     * Update a PENDING payment to SUCCESS or FAILED.
     * Validates that the payment is still PENDING and that the reason has at least 10 characters.
     *
     * @param id         the payment ID to update
     * @param requestDTO the new status and reason
     * @return the updated payment as a response DTO
     */
    PaymentResponseDTO updatePaymentStatus(Long id, ManualStatusRequestDTO requestDTO);

    /**
     * Cancel a PENDING payment transaction.
     *
     * @param id the payment ID to cancel
     * @return the cancelled payment as a response DTO
     */
    PaymentResponseDTO cancelPayment(Long id);

    /**
     * Process VNPAY Webhook IPN callback.
     *
     * @param request the HTTP request containing VNPAY parameters
     * @return JSON response indicating success or failure to VNPAY
     */
    ResponseEntity<String> processVnpayIpn(HttpServletRequest request);

    ResponseEntity<String> processVnpayReturn(HttpServletRequest request);
}
