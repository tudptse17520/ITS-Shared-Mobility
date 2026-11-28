package vn.edu.ut.its.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ut.its.constant.MessageConstants;
import vn.edu.ut.its.constant.RideSessionStatus;
import vn.edu.ut.its.dto.request.PricingPolicyRequestDTO;
import vn.edu.ut.its.dto.response.PricingPolicyResponseDTO;
import vn.edu.ut.its.entity.PricingPolicy;
import vn.edu.ut.its.entity.VehicleType;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.exception.ConflictException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.RideSessionRepository;
import vn.edu.ut.its.repository.PricingPolicyCrudRepository;
import vn.edu.ut.its.repository.VehicleTypeRepository;
import vn.edu.ut.its.service.PricingPolicyService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PricingPolicyServiceImpl implements PricingPolicyService {

    private final PricingPolicyCrudRepository pricingPolicyCrudRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final RideSessionRepository rideSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllPricingPolicies() {
        List<PricingPolicy> policies = pricingPolicyCrudRepository.findAll();

        List<PricingPolicyResponseDTO> data = policies.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("message", MessageConstants.PRICING_POLICY_LIST_SUCCESS);
        response.put("data", data);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPricingPoliciesByVehicleTypeId(Long vehicleTypeId) {
        List<PricingPolicy> policies = pricingPolicyCrudRepository.findByVehicleType_Id(vehicleTypeId);

        List<PricingPolicyResponseDTO> data = policies.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        if (data.isEmpty()) {
            response.put("message", "Chưa có bảng giá nào cho loại phương tiện này.");
        } else {
            response.put("message", MessageConstants.PRICING_POLICY_LIST_SUCCESS);
        }
        response.put("data", data);
        return response;
    }

    @Override
    public Map<String, Object> createPricingPolicy(PricingPolicyRequestDTO requestDTO) {
        VehicleType vehicleType = vehicleTypeRepository.findById(requestDTO.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageConstants.VEHICLE_TYPE_NOT_FOUND + requestDTO.getVehicleTypeId()));

        LocalDateTime effectiveDateTime = requestDTO.getEffectiveDate().atStartOfDay();

        if (pricingPolicyCrudRepository.existsByVehicleType_IdAndEffectiveDate(
                requestDTO.getVehicleTypeId(), effectiveDateTime)) {
            throw new ConflictException(MessageConstants.PRICING_POLICY_DUPLICATE);
        }

        PricingPolicy pricingPolicy = PricingPolicy.builder()
                .vehicleType(vehicleType)
                .basePrice(requestDTO.getBasePrice())
                .extraFeePerHour(requestDTO.getExtraFeePerHour())
                .effectiveDate(effectiveDateTime)
                .status(requestDTO.getStatus() != null ? requestDTO.getStatus() : vn.edu.ut.its.constant.PricingPolicyStatus.ACTIVE)
                .build();

        PricingPolicy savedPolicy = pricingPolicyCrudRepository.save(pricingPolicy);

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedPolicy.getId());
        response.put("message", MessageConstants.PRICING_POLICY_CREATE_SUCCESS);
        return response;
    }

    @Override
    public Map<String, Object> updatePricingPolicy(Long id, PricingPolicyRequestDTO requestDTO) {
        PricingPolicy pricingPolicy = pricingPolicyCrudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageConstants.PRICING_POLICY_NOT_FOUND + id));

        checkRideSessionConstraint(pricingPolicy.getVehicleType().getId(),
                MessageConstants.PRICING_POLICY_IN_USE_UPDATE);

        VehicleType vehicleType = vehicleTypeRepository.findById(requestDTO.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageConstants.VEHICLE_TYPE_NOT_FOUND + requestDTO.getVehicleTypeId()));

        LocalDateTime effectiveDateTime = requestDTO.getEffectiveDate().atStartOfDay();

        if (pricingPolicyCrudRepository.existsByVehicleType_IdAndEffectiveDateAndIdNot(
                requestDTO.getVehicleTypeId(), effectiveDateTime, id)) {
            throw new ConflictException(MessageConstants.PRICING_POLICY_DUPLICATE);
        }

        pricingPolicy.setVehicleType(vehicleType);
        pricingPolicy.setBasePrice(requestDTO.getBasePrice());
        pricingPolicy.setExtraFeePerHour(requestDTO.getExtraFeePerHour());
        pricingPolicy.setEffectiveDate(effectiveDateTime);
        if (requestDTO.getStatus() != null) {
            pricingPolicy.setStatus(requestDTO.getStatus());
        }

        PricingPolicy updatedPolicy = pricingPolicyCrudRepository.save(pricingPolicy);

        Map<String, Object> response = new HashMap<>();
        response.put("id", updatedPolicy.getId());
        response.put("message", MessageConstants.PRICING_POLICY_UPDATE_SUCCESS);
        return response;
    }

    @Override
    public Map<String, Object> deletePricingPolicy(Long id) {
        PricingPolicy pricingPolicy = pricingPolicyCrudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageConstants.PRICING_POLICY_NOT_FOUND + id));

        checkRideSessionConstraint(pricingPolicy.getVehicleType().getId(),
                MessageConstants.PRICING_POLICY_IN_USE_DELETE);

        pricingPolicyCrudRepository.delete(pricingPolicy);

        Map<String, Object> response = new HashMap<>();
        response.put("message", MessageConstants.PRICING_POLICY_DELETE_SUCCESS);
        return response;
    }

    // Giữ bảng giá ổn định cho các chuyến đi đang diễn ra.
    private void checkRideSessionConstraint(Long vehicleTypeId, String errorMessage) {
        boolean hasInProgressSession = rideSessionRepository
                .existsByVehicle_VehicleType_IdAndStatusAndEndedAtIsNull(vehicleTypeId, RideSessionStatus.IN_PROGRESS);

        if (hasInProgressSession) {
            throw new BusinessRuleViolationException(errorMessage);
        }
    }

    private PricingPolicyResponseDTO mapToResponseDTO(PricingPolicy pricingPolicy) {
        return PricingPolicyResponseDTO.builder()
                .id(pricingPolicy.getId())
                .vehicleTypeId(pricingPolicy.getVehicleType().getId())
                .basePrice(pricingPolicy.getBasePrice())
                .extraFeePerHour(pricingPolicy.getExtraFeePerHour())
                .effectiveDate(pricingPolicy.getEffectiveDate().toLocalDate())
                .status(pricingPolicy.getStatus())
                .build();
    }
}
