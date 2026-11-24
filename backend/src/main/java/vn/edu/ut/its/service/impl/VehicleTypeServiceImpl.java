package vn.edu.ut.its.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ut.its.constant.RideSessionStatus;
import vn.edu.ut.its.constant.VehicleTypeStatus;
import vn.edu.ut.its.dto.request.VehicleTypeRequestDTO;
import vn.edu.ut.its.dto.response.VehicleTypeResponseDTO;
import vn.edu.ut.its.entity.VehicleType;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.exception.ConflictException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.RideSessionRepository;
import vn.edu.ut.its.repository.VehicleTypeRepository;
import vn.edu.ut.its.service.VehicleTypeService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of VehicleTypeService.
 * Handles all CRUD operations and business rule validations (E1, E2, E3).
 * E3 check: chỉ kiểm tra có xe nào thuộc loại phương tiện đang đỗ trong bãi (RideSession IN_PROGRESS).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final RideSessionRepository rideSessionRepository;

    // ==================== GET - Lấy danh sách ====================

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeResponseDTO> getAllVehicleTypes() {
        return vehicleTypeRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // ==================== POST - Thêm mới ====================

    @Override
    public VehicleTypeResponseDTO createVehicleType(VehicleTypeRequestDTO requestDTO) {
        // E2: Check duplicate name
        if (vehicleTypeRepository.existsByTypeName(requestDTO.getTypeName())) {
            throw new ConflictException(
                    "Loại phương tiện '" + requestDTO.getTypeName() + "' đã tồn tại trong hệ thống.");
        }

        // Build entity with default ACTIVE status
        VehicleType vehicleType = VehicleType.builder()
                .typeName(requestDTO.getTypeName())
                .description(requestDTO.getDescription())
                .status(VehicleTypeStatus.ACTIVE)
                .build();

        VehicleType savedVehicleType = vehicleTypeRepository.save(vehicleType);
        return mapToResponseDTO(savedVehicleType);
    }

    // ==================== PUT - Cập nhật ====================

    @Override
    public VehicleTypeResponseDTO updateVehicleType(Long id, VehicleTypeRequestDTO requestDTO) {
        // Find existing entity or throw 404
        VehicleType vehicleType = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy loại phương tiện với ID: " + id));

        // E2: Check duplicate name (excluding this entity's own ID)
        if (vehicleTypeRepository.existsByTypeNameAndIdNot(requestDTO.getTypeName(), id)) {
            throw new ConflictException(
                    "Loại phương tiện '" + requestDTO.getTypeName() + "' đã tồn tại trong hệ thống.");
        }

        // Update fields
        vehicleType.setTypeName(requestDTO.getTypeName());
        vehicleType.setDescription(requestDTO.getDescription());
        if (requestDTO.getStatus() != null) {
            vehicleType.setStatus(requestDTO.getStatus());
        }

        VehicleType updatedVehicleType = vehicleTypeRepository.save(vehicleType);
        return mapToResponseDTO(updatedVehicleType);
    }

    // ==================== PATCH - Ngừng áp dụng ====================

    @Override
    public VehicleTypeResponseDTO deactivateVehicleType(Long id) {
        // Find existing entity or throw 404
        VehicleType vehicleType = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy loại phương tiện với ID: " + id));

        // E3: Kiểm tra có xe nào thuộc loại phương tiện này đang đỗ trong bãi không
        boolean hasActiveRide = rideSessionRepository
                .existsByVehicle_VehicleType_IdAndStatus(id, RideSessionStatus.IN_PROGRESS);
        if (hasActiveRide) {
            throw new BusinessRuleViolationException(
                    "Không thể ngừng áp dụng loại phương tiện này vì đang có xe thuộc danh mục này đang đỗ trong bãi.");
        }

        // All checks passed — deactivate
        vehicleType.setStatus(VehicleTypeStatus.INACTIVE);
        VehicleType deactivatedVehicleType = vehicleTypeRepository.save(vehicleType);
        return mapToResponseDTO(deactivatedVehicleType);
    }

    @Override
    public void deleteVehicleType(Long id) {
        VehicleType vehicleType = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy loại phương tiện với ID: " + id));
        try {
            vehicleTypeRepository.delete(vehicleType);
            vehicleTypeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleViolationException(
                    "Không thể xóa loại phương tiện đang được sử dụng bởi khu, bảng giá hoặc xe.");
        }
    }

    // ==================== Helper ====================

    /**
     * Map a VehicleType entity to a VehicleTypeResponseDTO.
     */
    private VehicleTypeResponseDTO mapToResponseDTO(VehicleType vehicleType) {
        return VehicleTypeResponseDTO.builder()
                .id(vehicleType.getId())
                .typeName(vehicleType.getTypeName())
                .description(vehicleType.getDescription())
                .status(vehicleType.getStatus())
                .build();
    }
}
