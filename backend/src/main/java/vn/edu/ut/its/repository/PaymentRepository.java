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
}
