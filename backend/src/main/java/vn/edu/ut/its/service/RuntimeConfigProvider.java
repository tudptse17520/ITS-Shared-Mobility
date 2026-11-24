package vn.edu.ut.its.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.ut.its.repository.SystemConfigRepository;

import java.math.BigDecimal;

/**
 * Provides runtime operational thresholds backed by dbo.system_config with safe defaults.
 */
@Service
@RequiredArgsConstructor
public class RuntimeConfigProvider {

    public static final String KEY_HOLD_DURATION_MINUTES = "HOLD_DURATION_MINUTES";
    public static final String KEY_LOW_BATTERY_THRESHOLD = "LOW_BATTERY_THRESHOLD";
    public static final String KEY_DISTANCE_RATE_PER_KM = "DISTANCE_RATE_PER_KM";
    public static final String KEY_SEARCH_RADIUS_KM = "SEARCH_RADIUS_KM";

    private static final int DEFAULT_HOLD_DURATION_MINUTES = 15;
    private static final int DEFAULT_LOW_BATTERY_THRESHOLD = 15;
    private static final BigDecimal DEFAULT_DISTANCE_RATE_PER_KM = BigDecimal.valueOf(2000);
    private static final double DEFAULT_SEARCH_RADIUS_KM = 5.0;

    private final SystemConfigRepository systemConfigRepository;

    public int getHoldDurationMinutes() {
        return systemConfigRepository.findByConfigKey(KEY_HOLD_DURATION_MINUTES)
                .map(c -> parseIntInRange(c.getConfigValue(), 1, 1440, DEFAULT_HOLD_DURATION_MINUTES))
                .orElse(DEFAULT_HOLD_DURATION_MINUTES);
    }

    public int getLowBatteryThreshold() {
        return systemConfigRepository.findByConfigKey(KEY_LOW_BATTERY_THRESHOLD)
                .map(c -> parseIntInRange(c.getConfigValue(), 1, 95, DEFAULT_LOW_BATTERY_THRESHOLD))
                .orElse(DEFAULT_LOW_BATTERY_THRESHOLD);
    }

    public BigDecimal getDistanceRatePerKm() {
        return systemConfigRepository.findByConfigKey(KEY_DISTANCE_RATE_PER_KM)
                .map(c -> parseNonNegativeDecimal(c.getConfigValue(), DEFAULT_DISTANCE_RATE_PER_KM))
                .orElse(DEFAULT_DISTANCE_RATE_PER_KM);
    }

    public double getDefaultSearchRadiusKm() {
        return systemConfigRepository.findByConfigKey(KEY_SEARCH_RADIUS_KM)
                .map(c -> parseDoubleMin(c.getConfigValue(), 0.5, DEFAULT_SEARCH_RADIUS_KM))
                .orElse(DEFAULT_SEARCH_RADIUS_KM);
    }

    private int parseIntInRange(String raw, int min, int max, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int val = Integer.parseInt(raw.trim());
            return (val >= min && val <= max) ? val : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private BigDecimal parseNonNegativeDecimal(String raw, BigDecimal fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            BigDecimal val = new BigDecimal(raw.trim());
            return val.compareTo(BigDecimal.ZERO) >= 0 ? val : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double parseDoubleMin(String raw, double min, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            double val = Double.parseDouble(raw.trim());
            return val >= min ? val : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

