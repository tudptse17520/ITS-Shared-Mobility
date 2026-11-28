package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.entity.PricingPolicy;

/**
 * Repository for PricingPolicy entity.
 */
@Repository
public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, Long> {

    @Modifying
    @Query(value = "DELETE FROM pricing_policy", nativeQuery = true)
    void hardDeleteAllInBatch();

    /**
     * Tìm chính sách giá có hiệu lực mới nhất của một loại phương tiện tại thời điểm chỉ định.
     *
     * @param vehicleTypeId mã loại phương tiện
     * @param dateTime      thời điểm cần áp dụng chính sách giá (thường là lúc bắt đầu chuyến đi hoặc thời điểm hiện tại)
     * @return một Optional chứa PricingPolicy nếu tìm thấy
     */
    java.util.Optional<PricingPolicy> findFirstByVehicleType_IdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(Long vehicleTypeId, java.time.LocalDateTime dateTime);

    @Query("SELECT p FROM PricingPolicy p JOIN FETCH p.vehicleType vt WHERE p.effectiveDate <= :dateTime ORDER BY p.effectiveDate DESC")
    java.util.List<PricingPolicy> findEffectivePoliciesUpTo(@org.springframework.data.repository.query.Param("dateTime") java.time.LocalDateTime dateTime);
}
