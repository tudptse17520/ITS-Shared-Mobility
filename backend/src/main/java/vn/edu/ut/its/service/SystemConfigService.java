package vn.edu.ut.its.service;

import vn.edu.ut.its.dto.request.SystemConfigRequest;
import vn.edu.ut.its.dto.response.SystemConfigListResponse;
import vn.edu.ut.its.dto.response.SystemConfigUpdateResponse;

/**
 * Service interface for managing system configurations.
 */
public interface SystemConfigService {
    SystemConfigListResponse getConfigurations(String keyword);
    SystemConfigUpdateResponse updateConfiguration(int id, SystemConfigRequest request);
}
