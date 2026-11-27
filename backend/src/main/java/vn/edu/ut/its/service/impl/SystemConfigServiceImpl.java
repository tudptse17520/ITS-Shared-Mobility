package vn.edu.ut.its.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ut.its.dto.request.SystemConfigRequest;
import vn.edu.ut.its.dto.response.SystemConfigListResponse;
import vn.edu.ut.its.dto.response.SystemConfigResponse;
import vn.edu.ut.its.dto.response.SystemConfigUpdateResponse;
import vn.edu.ut.its.entity.SystemConfig;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.SystemConfigRepository;
import vn.edu.ut.its.service.RuntimeConfigProvider;
import vn.edu.ut.its.service.SystemConfigService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing system configurations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public SystemConfigListResponse getConfigurations(String keyword) {
        List<SystemConfig> configs;
        if (keyword != null && !keyword.trim().isEmpty()) {
            configs = systemConfigRepository.searchByKeyword(keyword.trim());
        } else {
            configs = systemConfigRepository.findAll();
        }

        List<SystemConfigResponse> responseData = configs.stream().map(this::mapToResponse).collect(Collectors.toList());

        return SystemConfigListResponse.builder()
                .totalItems(responseData.size())
                .data(responseData)
                .message("Fetched configurations successfully")
                .build();
    }

    @Override
    @Transactional
    public SystemConfigUpdateResponse updateConfiguration(int id, SystemConfigRequest request) {
        SystemConfig config = systemConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tham số cấu hình với ID: " + id));

        String rawValue = request.getConfigValue() != null ? request.getConfigValue().trim() : "";
        validateConfigValue(config.getConfigKey(), rawValue);

        config.setConfigValue(rawValue);
        config.setDescription(request.getDescription());

        systemConfigRepository.save(config);

        return SystemConfigUpdateResponse.builder()
                .id(id)
                .message("Cập nhật tham số cấu hình thành công.")
                .build();
    }

    private void validateConfigValue(String key, String value) {
        if (key == null) {
            return;
        }
        try {
            switch (key) {
                case RuntimeConfigProvider.KEY_HOLD_DURATION_MINUTES -> {
                    int minutes = Integer.parseInt(value);
                    if (minutes <= 0 || minutes > 1440) {
                        throw new BusinessRuleViolationException("HOLD_DURATION_MINUTES phải nằm trong khoảng 1 đến 1440 phút.");
                    }
                }
                case RuntimeConfigProvider.KEY_LOW_BATTERY_THRESHOLD -> {
                    int pct = Integer.parseInt(value);
                    if (pct < 1 || pct > 95) {
                        throw new BusinessRuleViolationException("LOW_BATTERY_THRESHOLD phải nằm trong khoảng 1% đến 95%.");
                    }
                }
                case RuntimeConfigProvider.KEY_DISTANCE_RATE_PER_KM -> {
                    BigDecimal rate = new BigDecimal(value);
                    if (rate.compareTo(BigDecimal.ZERO) < 0) {
                        throw new BusinessRuleViolationException("DISTANCE_RATE_PER_KM phải lớn hơn hoặc bằng 0.");
                    }
                }
                case RuntimeConfigProvider.KEY_SEARCH_RADIUS_KM -> {
                    double radius = Double.parseDouble(value);
                    if (radius <= 0) {
                        throw new BusinessRuleViolationException("SEARCH_RADIUS_KM phải lớn hơn 0.");
                    }
                }
                default -> {
                    if (value.isBlank()) {
                        throw new BusinessRuleViolationException("Giá trị cấu hình không được để trống.");
                    }
                }
            }
        } catch (NumberFormatException ex) {
            throw new BusinessRuleViolationException("Giá trị '" + value + "' không hợp lệ cho tham số " + key + ".");
        }
    }

    private SystemConfigResponse mapToResponse(SystemConfig config) {
        return SystemConfigResponse.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .description(config.getDescription())
                .build();
    }
}
