package vn.edu.ut.its.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import vn.edu.ut.its.config.VnpayConfig;
import vn.edu.ut.its.constant.BookingStatus;
import vn.edu.ut.its.constant.FeeType;
import vn.edu.ut.its.constant.PaymentMethod;
import vn.edu.ut.its.constant.PaymentStatus;
import vn.edu.ut.its.constant.RideSessionStatus;
import vn.edu.ut.its.dto.request.ManualStatusRequestDTO;
import vn.edu.ut.its.dto.request.PaymentRequestDTO;
import vn.edu.ut.its.dto.response.PaymentListResponseDTO;
import vn.edu.ut.its.dto.response.PaymentResponseDTO;
import vn.edu.ut.its.entity.Booking;
import vn.edu.ut.its.entity.RideSession;
import vn.edu.ut.its.entity.Payment;
import vn.edu.ut.its.entity.PricingPolicy;
import vn.edu.ut.its.entity.User;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.exception.InvalidStateTransitionException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.BookingRepository;
import vn.edu.ut.its.repository.RideSessionRepository;
import vn.edu.ut.its.repository.PaymentCustomRepository;
import vn.edu.ut.its.repository.PaymentRepository;
import vn.edu.ut.its.repository.UserRepository;
import vn.edu.ut.its.security.SecurityUtils;
import vn.edu.ut.its.service.PaymentService;
import vn.edu.ut.its.util.VnpayUtil;

/**
 * Implementation of PaymentService. Handles all payment business logic
 * Supports booking deposits, ride fares, cash and electronic payments.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentCustomRepository paymentCustomRepository;
    private final RideSessionRepository rideSessionRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VnpayConfig vnpayConfig;

    private static final DateTimeFormatter RESPONSE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== CREATE - Tạo thanh toán ====================
    @Override
    public PaymentResponseDTO createPayment(PaymentRequestDTO requestDTO, HttpServletRequest request) {
        // 1. Validate mutual exclusivity: phải có đúng 1 trong rideSessionId hoặc bookingId
        validateMutualExclusivity(requestDTO);

        // 2. Validate fee_type enum
        FeeType feeType = requestDTO.getFeeType();
        if (feeType == FeeType.HISTORICAL_ADJUSTMENT) {
            throw new BusinessRuleViolationException("Hệ thống chỉ nhận thanh toán cước chuyến đi hoặc tiền cọc đặt giữ xe.");
        }

        // 3. Validate cross-mapping: fee_type MUST match the provided ID type
        if (requestDTO.getBookingId() != null && feeType != FeeType.BOOKING_DEPOSIT) {
            throw new BusinessRuleViolationException(
                    "Giao dịch sai: Khi thanh toán cho đơn đặt chỗ (booking_id), loại phí bắt buộc phải là BOOKING_DEPOSIT.");
        }
        if (requestDTO.getRideSessionId() != null && feeType == FeeType.BOOKING_DEPOSIT) {
            throw new BusinessRuleViolationException(
                    "Giao dịch sai: Khi thanh toán cho chuyến đi (ride_session_id), loại phí không được là BOOKING_DEPOSIT.");
        }

        // 4. Validate payment_method enum
        PaymentMethod paymentMethod = requestDTO.getPaymentMethod();

        // 5. E3 Check (Overpayment Violation) - Ride_Fee
        RideSession rideSession = null;
        Booking booking = null;

        if (requestDTO.getRideSessionId() != null) {
            rideSession = rideSessionRepository.findById(requestDTO.getRideSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                    "Không tìm thấy chuyến đi với ID: " + requestDTO.getRideSessionId()));

            SecurityUtils.assertAdminOrOwner(
                    rideSession.getUser() != null ? rideSession.getUser().getId() : null,
                    userRepository,
                    "Bạn không có quyền thanh toán cho chuyến đi của người dùng khác.");

            if (feeType == FeeType.RIDE_FEE) {
                validateOverpayment(requestDTO, rideSession);
            }
            if (paymentMethod == PaymentMethod.Vnpay) {
                failPriorPendingPaymentsForRide(rideSession.getId());
            }
        }

        if (requestDTO.getBookingId() != null) {
            booking = bookingRepository.findById(requestDTO.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                    "Không tìm thấy đơn đặt chỗ với ID: " + requestDTO.getBookingId()));

            SecurityUtils.assertAdminOrOwner(
                    booking.getUser() != null ? booking.getUser().getId() : null,
                    userRepository,
                    "Bạn không có quyền thanh toán cho lượt đặt giữ của người dùng khác.");

            validateBookingDeposit(requestDTO.getAmount(), booking);
            if (paymentMethod == PaymentMethod.Vnpay) {
                failPriorPendingPaymentsForBooking(booking.getId());
            }
        }

        // 6. Build Payment entity
        Payment payment = Payment.builder()
                .amount(requestDTO.getAmount())
                .paymentMethod(requestDTO.getPaymentMethod())
                .feeType(feeType)
                .rideSession(rideSession)
                .booking(booking)
                .status(PaymentStatus.PENDING)
                .build();

        // 7. Cash → auto SUCCESS + paymentTime = now()
        if (paymentMethod == PaymentMethod.Cash) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentTime(LocalDateTime.now());
        } else {
            payment.setPaymentTime(LocalDateTime.now());
        }

        // 8. Save to DB
        Payment savedPayment = paymentRepository.save(payment);

        PaymentResponseDTO responseDTO = mapToResponseDTO(savedPayment);

        // 9. Generate VNPAY URL if Vnpay method is selected
        if (paymentMethod == PaymentMethod.Vnpay) {
            String vnpayUrl = generateVnpayUrl(savedPayment, request);
            responseDTO.setPaymentUrl(vnpayUrl);
        }

        // 10. Auto-complete RideSession if fully paid (for Cash payments)
        if (savedPayment.getStatus() == PaymentStatus.SUCCESS && savedPayment.getRideSession() != null) {
            checkAndCompleteRideSession(savedPayment.getRideSession());
        }

        return responseDTO;
    }

    // ==================== GET - Danh sách giao dịch ====================
    @Override
    @Transactional(readOnly = true)
    public PaymentListResponseDTO getPayments(String paymentMethod, String status, String fromDate) {
        // Convert status String to PaymentStatus enum (nullable)
        PaymentStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = PaymentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleViolationException(
                        "Trạng thái '" + status + "' không hợp lệ. Các giá trị hợp lệ: PENDING, SUCCESS, FAILED.");
            }
        }

        // Convert fromDate String (DD-MM-YYYY) to LocalDateTime (nullable)
        LocalDateTime parsedFromDateTime = null;
        if (fromDate != null && !fromDate.isBlank()) {
            LocalDate parsedFromDate = LocalDate.parse(fromDate, REQUEST_DATE_FORMAT);
            parsedFromDateTime = parsedFromDate.atStartOfDay();
        }

        List<Payment> payments = paymentCustomRepository.findByFilters(paymentMethod, parsedStatus, parsedFromDateTime);

        User currentUser = SecurityUtils.getAuthenticatedUser(userRepository).orElse(null);
        if (currentUser != null && !SecurityUtils.isAdmin(currentUser)) {
            Long uid = currentUser.getId();
            payments = payments.stream()
                    .filter(p -> isOwnedByUser(p, uid))
                    .collect(Collectors.toList());
        }

        List<PaymentResponseDTO> data = payments.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        return PaymentListResponseDTO.builder()
                .totalItems(data.size())
                .message("Lấy danh sách giao dịch thành công")
                .data(data)
                .build();
    }

    // ==================== GET - Chi tiết biên lai ====================
    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Không tìm thấy giao dịch thanh toán với ID: " + id));

        SecurityUtils.assertAdminOrOwner(resolvePaymentOwnerId(payment), userRepository,
                "Bạn không có quyền xem biên lai thanh toán của người dùng khác.");

        return mapToResponseDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public vn.edu.ut.its.dto.response.PaymentDebtResponseDTO getRemainingDebt(Long sessionId) {
        RideSession session = rideSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + sessionId));

        SecurityUtils.assertAdminOrOwner(
                session.getUser() != null ? session.getUser().getId() : null,
                userRepository,
                "Bạn không có quyền xem công nợ chuyến đi của người dùng khác.");

        BigDecimal totalFee = requireFinalRideFee(session);

        BigDecimal sumPaid = paymentCustomRepository.sumSuccessfulAmountBySessionId(session.getId());
        if (session.getBooking() != null) {
            sumPaid = sumPaid.add(paymentCustomRepository.sumSuccessfulAmountByBookingId(session.getBooking().getId()));
        }

        BigDecimal remainingDebt = totalFee.subtract(sumPaid);
        if (remainingDebt.compareTo(BigDecimal.ZERO) < 0) {
            remainingDebt = BigDecimal.ZERO;
        }

        return vn.edu.ut.its.dto.response.PaymentDebtResponseDTO.builder()
                .sessionId(session.getId())
                .totalFee(totalFee)
                .paidFee(sumPaid)
                .remainingFee(remainingDebt)
                .build();
    }

    // ==================== PATCH - Staff cập nhật thủ công ====================
    @Override
    public PaymentResponseDTO updatePaymentStatus(Long id, ManualStatusRequestDTO requestDTO) {
        // 1. Find entity
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Không tìm thấy giao dịch thanh toán với ID: " + id));

        // 2. Check current status must be PENDING
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Giao dịch này đã ở trạng thái " + payment.getStatus() + ", không thể can thiệp thay đổi.");
        }

        // 3. Validate audit note: not blank and minimum 10 characters
        if (requestDTO.getNote() == null || requestDTO.getNote().isBlank()
                || requestDTO.getNote().trim().length() < 10) {
            throw new InvalidStateTransitionException(
                    "Nhân viên bắt buộc phải nhập lý do chi tiết khi thao tác cập nhật thủ công.",
                    "MISSING_AUDIT_NOTE");
        }

        // 4. Parse and validate new status (only SUCCESS or FAILED allowed)
        PaymentStatus newStatus;
        try {
            newStatus = requestDTO.getStatus();
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleViolationException(
                    "Trạng thái '" + requestDTO.getStatus() + "' không hợp lệ. Chỉ chấp nhận SUCCESS hoặc FAILED.");
        }

        if (newStatus != PaymentStatus.SUCCESS && newStatus != PaymentStatus.FAILED) {
            throw new BusinessRuleViolationException(
                    "Chỉ được phép cập nhật thủ công sang trạng thái SUCCESS hoặc FAILED.");
        }

        // 5. Update status
        payment.setStatus(newStatus);
        if (newStatus == PaymentStatus.SUCCESS) {
            payment.setPaymentTime(LocalDateTime.now());
        }

        Payment updatedPayment = paymentRepository.save(payment);

        if (newStatus == PaymentStatus.SUCCESS && updatedPayment.getRideSession() != null) {
            checkAndCompleteRideSession(updatedPayment.getRideSession());
        }

        return mapToResponseDTO(updatedPayment);
    }

    // ==================== PATCH - Hủy giao dịch ====================
    @Override
    public PaymentResponseDTO cancelPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Không tìm thấy giao dịch thanh toán với ID: " + id));

        SecurityUtils.assertAdminOrOwner(resolvePaymentOwnerId(payment), userRepository,
                "Bạn không có quyền hủy giao dịch thanh toán của người dùng khác.");

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Chỉ có thể hủy giao dịch đang ở trạng thái PENDING. Giao dịch hiện tại đang ở trạng thái "
                    + payment.getStatus() + ".");
        }

        payment.setStatus(PaymentStatus.FAILED);
        Payment cancelledPayment = paymentRepository.save(payment);
        return mapToResponseDTO(cancelledPayment);
    }

    // ==================== VNPAY IPN Webhook ====================
    @Override
    public ResponseEntity<String> processVnpayIpn(HttpServletRequest request) {
        try {
            if (vnpayConfig.getSecretKey() == null || vnpayConfig.getSecretKey().isBlank()) {
                return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}");
            }

            Map<String, String> fields = new HashMap<>();
            for (java.util.Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
                String fieldName = params.nextElement();
                String fieldValue = request.getParameter(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    fields.put(fieldName, fieldValue);
                }
            }

            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            if (vnp_SecureHash == null || vnp_SecureHash.isBlank()) {
                return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}");
            }
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");

            // Validate signature
            String signValue = VnpayUtil.hashAllFields(fields, vnpayConfig.getSecretKey());
            if (!signValue.equalsIgnoreCase(vnp_SecureHash)) {
                return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}");
            }

            Long paymentId;
            try {
                paymentId = Long.parseLong(fields.get("vnp_TxnRef"));
            } catch (Exception e) {
                return ResponseEntity.ok("{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
            }

            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                return ResponseEntity.ok("{\"RspCode\":\"01\",\"Message\":\"Order not found\"}");
            }

            BigDecimal vnpAmount;
            try {
                vnpAmount = new BigDecimal(fields.get("vnp_Amount")).divide(new BigDecimal("100"));
            } catch (Exception e) {
                return ResponseEntity.ok("{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}");
            }
            if (payment.getAmount().compareTo(vnpAmount) != 0) {
                return ResponseEntity.ok("{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}");
            }

            if (payment.getStatus() != PaymentStatus.PENDING) {
                return ResponseEntity.ok("{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}");
            }

            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                // Guard against duplicate settlement after the ride/booking is already paid
                if (payment.getRideSession() != null) {
                    RideSession session = payment.getRideSession();
                    if (session.getStatus() == RideSessionStatus.COMPLETED || remainingDebtForSession(session).compareTo(payment.getAmount()) < 0) {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                        return ResponseEntity.ok("{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}");
                    }
                }
                if (payment.getBooking() != null && paymentCustomRepository.sumSuccessfulAmountByBookingId(payment.getBooking().getId()).signum() > 0) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    return ResponseEntity.ok("{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}");
                }
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaymentTime(LocalDateTime.now());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }

            paymentRepository.save(payment);

            // If this payment was for a Booking, confirm it
            if (payment.getStatus() == PaymentStatus.SUCCESS && payment.getBooking() != null) {
                Booking booking = payment.getBooking();
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
            }

            // If this payment was for a RideSession, check if fully paid to complete it
            if (payment.getStatus() == PaymentStatus.SUCCESS && payment.getRideSession() != null) {
                checkAndCompleteRideSession(payment.getRideSession());
            }

            return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
        } catch (Exception e) {
            return ResponseEntity.ok("{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}");
        }
    }

    @Override
    public ResponseEntity<String> processVnpayReturn(HttpServletRequest request) {
        ResponseEntity<String> ipnResult = processVnpayIpn(request);
        String ipnBody = ipnResult.getBody() != null ? ipnResult.getBody() : "";
        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef = request.getParameter("vnp_TxnRef");
        boolean ipnConfirmed = ipnBody.contains("\"RspCode\":\"00\"") || ipnBody.contains("\"RspCode\":\"02\"");
        boolean success = "00".equals(responseCode) && ipnConfirmed;
        String title = success ? "Thanh toán VNPay thành công!" : "Thanh toán VNPay chưa hoàn tất";
        String color = success ? "#16a34a" : "#dc2626";
        String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>%s</title>
                  <style>
                    body { font-family: system-ui, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background: #f8fafc; }
                    .card { background: #fff; padding: 28px; border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.08); text-align: center; max-width: 360px; }
                    h2 { color: %s; margin-bottom: 8px; }
                    p { color: #475569; font-size: 14px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h2>%s</h2>
                    <p>Mã giao dịch: <strong>#%s</strong></p>
                    <p>Bạn có thể quay lại ứng dụng ITS Shared Mobility để tiếp tục.</p>
                  </div>
                </body>
                </html>
                """.formatted(title, color, title, txnRef != null ? txnRef : "N/A");
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    // ==================== Private Helpers ====================
    private String generateVnpayUrl(Payment payment, HttpServletRequest request) {
        String vnp_Version = VnpayConfig.VNP_VERSION;
        String vnp_Command = VnpayConfig.VNP_COMMAND;
        String vnp_TxnRef = String.valueOf(payment.getId());
        String vnp_IpAddr = VnpayUtil.getIpAddress(request);
        String vnp_TmnCode = vnpayConfig.getVnpTmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);

        // VNPAY expects amount * 100
        BigDecimal amountInVND = payment.getAmount().multiply(new BigDecimal("100"));
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVND.longValue()));

        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);

        // Order Info
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");

        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getVnpReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        String queryUrl = VnpayUtil.getPaymentURL(vnp_Params, true);
        String hashData = VnpayUtil.getPaymentURL(vnp_Params, false);
        String vnp_SecureHash = VnpayUtil.hmacSHA512(vnpayConfig.getSecretKey(), hashData);
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnpayConfig.getVnpPayUrl() + "?" + queryUrl;
    }

    /**
     * Validate rằng client phải truyền đúng 1 trong 2 tham số rideSessionId
     * hoặc bookingId.
     */
    private void validateMutualExclusivity(PaymentRequestDTO dto) {
        boolean hasSession = dto.getRideSessionId() != null;
        boolean hasBooking = dto.getBookingId() != null;

        if (hasSession && hasBooking) {
            throw new BusinessRuleViolationException(
                    "Không thể truyền đồng thời cả ride_session_id và booking_id. Vui lòng chỉ truyền một trong hai.");
        }
        if (!hasSession && !hasBooking) {
            throw new BusinessRuleViolationException(
                    "Bắt buộc phải truyền ride_session_id hoặc booking_id.");
        }
    }

    /**
     * E2: Validate Fixed Fee Mismatch. Kiểm tra số tiền thanh toán phải khớp
     * 100% với mức niêm yết.
     */
    private void validateBookingDeposit(BigDecimal amount, Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("Đơn đặt giữ xe hiện ở trạng thái " + booking.getStatus() + ", không thể thanh toán tiền cọc.");
        }
        if (paymentCustomRepository.sumSuccessfulAmountByBookingId(booking.getId()).signum() > 0) {
            throw new BusinessRuleViolationException("Đơn đặt giữ xe này đã được thanh toán tiền cọc trước đó.");
        }
        if (booking.getVehicle() == null || booking.getVehicle().getVehicleType() == null) {
            throw new BusinessRuleViolationException("Không xác định được loại xe để tra cứu tiền cọc.");
        }
        List<PricingPolicy> policies = booking.getVehicle().getVehicleType().getPricingPolicies();
        if (policies == null || policies.isEmpty()) {
            throw new BusinessRuleViolationException("Chưa có bảng giá cho loại xe này.");
        }

        LocalDateTime now = LocalDateTime.now();
        PricingPolicy latestPolicy = policies.stream()
                .filter(policy -> !policy.getEffectiveDate().isAfter(now))
                .max(Comparator.comparing(PricingPolicy::getEffectiveDate))
                .orElseThrow(() -> new BusinessRuleViolationException("Không tìm thấy bảng giá đang có hiệu lực."));
        if (amount.compareTo(latestPolicy.getBasePrice()) != 0) {
            throw new BusinessRuleViolationException(
                    "Số tiền thanh toán không khớp với tiền cọc đặt giữ xe.");
        }
    }

    /**
     * E3: Validate Overpayment Violation. Kiểm tra tổng tiền thanh toán không
     * vượt quá total_fee còn nợ của chuyến đi.
     */
    private void validateOverpayment(PaymentRequestDTO dto, RideSession session) {
        if (session.getStatus() == RideSessionStatus.COMPLETED) {
            throw new BusinessRuleViolationException("Chuyến đi này đã hoàn tất thanh toán.");
        }
        BigDecimal remainingDebt = remainingDebtForSession(session);
        if (remainingDebt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Chuyến đi này không còn dư nợ cần thanh toán.");
        }
        if (dto.getAmount().compareTo(remainingDebt) > 0) {
            throw new BusinessRuleViolationException(
                    "Tổng số tiền thanh toán vượt quá cước chuyến đi.");
        }
    }

    private BigDecimal remainingDebtForSession(RideSession session) {
        BigDecimal totalFee = requireFinalRideFee(session);
        BigDecimal sumPaid = paymentCustomRepository.sumSuccessfulAmountBySessionId(session.getId());
        if (session.getBooking() != null) {
            sumPaid = sumPaid.add(paymentCustomRepository.sumSuccessfulAmountByBookingId(session.getBooking().getId()));
        }
        BigDecimal remaining = totalFee.subtract(sumPaid);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    private void failPriorPendingPaymentsForRide(Long sessionId) {
        List<Payment> pending = paymentRepository.findByRideSession_IdAndStatus(sessionId, PaymentStatus.PENDING);
        if (!pending.isEmpty()) {
            pending.forEach(p -> p.setStatus(PaymentStatus.FAILED));
            paymentRepository.saveAll(pending);
        }
    }

    private void failPriorPendingPaymentsForBooking(Long bookingId) {
        List<Payment> pending = paymentRepository.findByBooking_IdAndStatus(bookingId, PaymentStatus.PENDING);
        if (!pending.isEmpty()) {
            pending.forEach(p -> p.setStatus(PaymentStatus.FAILED));
            paymentRepository.saveAll(pending);
        }
    }

    private Long resolvePaymentOwnerId(Payment payment) {
        if (payment.getRideSession() != null && payment.getRideSession().getUser() != null) {
            return payment.getRideSession().getUser().getId();
        }
        if (payment.getBooking() != null && payment.getBooking().getUser() != null) {
            return payment.getBooking().getUser().getId();
        }
        return null;
    }

    private boolean isOwnedByUser(Payment payment, Long userId) {
        Long ownerId = resolvePaymentOwnerId(payment);
        return ownerId != null && ownerId.equals(userId);
    }

    private BigDecimal requireFinalRideFee(RideSession session) {
        if (session.getEndedAt() == null || session.getTotalFee() == null) {
            throw new BusinessRuleViolationException("Vui lòng kết thúc chuyến đi trước khi thanh toán.");
        }
        return session.getTotalFee();
    }

    /**
     * Tự động kiểm tra và chuyển trạng thái RideSession sang COMPLETED nếu
     * đã thanh toán đủ.
     */
    private void checkAndCompleteRideSession(RideSession session) {
        if (session.getEndedAt() == null || session.getTotalFee() == null
                || session.getStatus() == vn.edu.ut.its.constant.RideSessionStatus.COMPLETED) {
            return;
        }

        BigDecimal sumPaid = paymentCustomRepository.sumSuccessfulAmountBySessionId(session.getId());
        if (session.getBooking() != null) {
            sumPaid = sumPaid.add(paymentCustomRepository.sumSuccessfulAmountByBookingId(session.getBooking().getId()));
        }
        BigDecimal remainingDebt = session.getTotalFee().subtract(sumPaid);

        if (remainingDebt.compareTo(BigDecimal.ZERO) <= 0) {
            session.setStatus(vn.edu.ut.its.constant.RideSessionStatus.COMPLETED);
            rideSessionRepository.save(session);
        }
    }



    /**
     * Map Payment entity to PaymentResponseDTO.
     */
    private PaymentResponseDTO mapToResponseDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .rideSessionId(payment.getRideSession() != null ? payment.getRideSession().getId() : null)
                .bookingId(payment.getBooking() != null ? payment.getBooking().getId() : null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .feeType(payment.getFeeType())
                .paymentTime(payment.getPaymentTime() != null
                        ? payment.getPaymentTime().format(RESPONSE_DATE_FORMAT)
                        : null)
                .status(payment.getStatus())
                .build();
    }
}
