package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.constant.PaymentStatus;
import vn.edu.ut.its.entity.Payment;

import java.util.List;

/**
 * Repository interface for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByRideSession_IdAndStatus(Long rideSessionId, PaymentStatus status);

    List<Payment> findByBooking_IdAndStatus(Long bookingId, PaymentStatus status);

    @org.springframework.data.jpa.repository.Query("""
            SELECT DISTINCT p FROM Payment p
            LEFT JOIN FETCH p.rideSession rs
            LEFT JOIN FETCH rs.vehicle rsv
            LEFT JOIN FETCH rsv.vehicleType
            LEFT JOIN FETCH p.booking bk
            LEFT JOIN FETCH bk.vehicle bkv
            LEFT JOIN FETCH bkv.vehicleType
            WHERE p.status = :status
              AND p.feeType <> vn.edu.ut.its.constant.FeeType.HISTORICAL_ADJUSTMENT
              AND p.paymentTime >= :start
              AND p.paymentTime < :exclusiveEnd
            """)
    List<Payment> findRevenuePaymentsInRange(
            @org.springframework.data.repository.query.Param("status") PaymentStatus status,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("exclusiveEnd") java.time.LocalDateTime exclusiveEnd);
}
