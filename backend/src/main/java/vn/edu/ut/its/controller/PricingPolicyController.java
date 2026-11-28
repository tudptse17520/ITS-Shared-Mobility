package vn.edu.ut.its.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.edu.ut.its.constant.MessageConstants;
import vn.edu.ut.its.dto.request.PricingPolicyRequestDTO;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.service.PricingPolicyService;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/pricing-policies")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Pricing Policy")
public class PricingPolicyController {

    private final PricingPolicyService pricingPolicyService;

    /**
     * Lấy danh sách toàn bộ bảng giá trong hệ thống.
     *
     * @return HTTP 200 kèm danh sách bảng giá
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPricingPolicies() {
        Map<String, Object> response = pricingPolicyService.getAllPricingPolicies();
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách bảng giá lọc theo vehicle_type_id.
     * Trả về thông báo nếu loại xe chưa có bảng giá.
     *
     * @param vehicleTypeId mã loại xe để lọc
     * @return HTTP 200 kèm danh sách bảng giá
     */
    @GetMapping("/vehicle-type/{vehicleTypeId}")
    public ResponseEntity<Map<String, Object>> getPricingPoliciesByVehicleTypeId(
            @PathVariable Long vehicleTypeId) {
        Map<String, Object> response = pricingPolicyService.getPricingPoliciesByVehicleTypeId(vehicleTypeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Thêm mới một bảng giá.
     *
     * @param requestDTO dữ liệu bảng giá (đã validate)
     * @return HTTP 201 với { id, message }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createPricingPolicy(
            @Valid @RequestBody PricingPolicyRequestDTO requestDTO) {
        Map<String, Object> response = pricingPolicyService.createPricingPolicy(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cập nhật thông số giá cơ bản, phí phụ thu hoặc ngày bắt đầu hiệu lực.
     *
     * @param id         ID bảng giá cần cập nhật (path variable)
     * @param requestDTO dữ liệu cập nhật (đã validate)
     * @return HTTP 200 với { id, message }
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePricingPolicy(
            @PathVariable Long id,
            @Valid @RequestBody PricingPolicyRequestDTO requestDTO) {
        try {
            Map<String, Object> response = pricingPolicyService.updatePricingPolicy(id, requestDTO);
            return ResponseEntity.ok(response);
        } catch (BusinessRuleViolationException ex) {
            return buildBusinessRuleViolationResponse(ex.getMessage());
        }
    }

    /**
     * Xóa một chính sách tính phí khỏi hệ thống.
     *
     * @param id ID bảng giá cần xóa (path variable)
     * @return HTTP 200 với { message } nếu thành công
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePricingPolicy(@PathVariable Long id) {
        try {
            Map<String, Object> response = pricingPolicyService.deletePricingPolicy(id);
            return ResponseEntity.ok(response);
        } catch (BusinessRuleViolationException ex) {
            return buildBusinessRuleViolationResponse(ex.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> buildBusinessRuleViolationResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", MessageConstants.BUSINESS_RULE_VIOLATION);
        errorResponse.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
