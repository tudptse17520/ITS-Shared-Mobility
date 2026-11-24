package vn.edu.ut.its;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.edu.ut.its.dto.mobility.ReserveVehicleRequestDTO;
import vn.edu.ut.its.entity.User;
import vn.edu.ut.its.entity.Vehicle;
import vn.edu.ut.its.repository.UserRepository;
import vn.edu.ut.its.repository.VehicleRepository;
import vn.edu.ut.its.service.SharedMobilityService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiểm thử thực nghiệm Concurrency Control (Chương 5 Dàn ý ITS):
 * Giả lập 50 người dùng đồng thời cùng gửi yêu cầu đặt giữ 1 chiếc xe duy nhất (ITS-EB-001).
 * Đảm bảo cơ chế Pessimistic Write Lock ngăn chặn tuyệt đối lỗi Double Booking.
 */
@SpringBootTest(properties = "its.demo.seed=true")
class ConcurrencyReservationTest {

    @Autowired
    private SharedMobilityService sharedMobilityService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testConcurrentReservationOfSingleVehicle() throws Exception {
        Vehicle targetVehicle = vehicleRepository.findByVehicleCode("ITS-EB-001")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy xe ITS-EB-001"));
        User testUser = userRepository.findByUsername("user")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user mẫu"));

        // Đảm bảo xe đang AVAILABLE trước khi test
        if ("RESERVED".equalsIgnoreCase(targetVehicle.getFleetStatus())) {
            sharedMobilityService.cancelReservation(targetVehicle.getId());
        }

        int totalThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Long> latenciesMs = java.util.Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    long t0 = System.nanoTime();
                    sharedMobilityService.reserveVehicle(ReserveVehicleRequestDTO.builder()
                            .vehicleId(targetVehicle.getId())
                            .userId(testUser.getId())
                            .holdMinutes(15)
                            .build());
                    long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
                    latenciesMs.add(elapsedMs);
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    conflictCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await();
        long suiteStart = System.nanoTime();
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get();
        }
        long suiteElapsedMs = (System.nanoTime() - suiteStart) / 1_000_000;
        executor.shutdown();

        Vehicle updated = vehicleRepository.findById(targetVehicle.getId()).orElseThrow();
        double avgLatency = latenciesMs.stream().mapToLong(Long::longValue).average().orElse(0.0);

        System.out.println("============================================================");
        System.out.println("KET QUA THUC NGHIEM CONCURRENCY CONTROL (50 CONCURRENT USERS)");
        System.out.println("Target Vehicle      : " + updated.getVehicleCode() + " (ID=" + updated.getId() + ")");
        System.out.println("Total Requests      : " + totalThreads);
        System.out.println("Success (200 OK)    : " + successCount.get());
        System.out.println("Rejected (409/Lock) : " + conflictCount.get());
        System.out.println("Final Fleet Status  : " + updated.getFleetStatus());
        System.out.println("Total Suite Time    : " + suiteElapsedMs + " ms");
        System.out.println("Winner Latency      : " + String.format("%.2f", avgLatency) + " ms");
        System.out.println("============================================================");

        try {
            assertEquals(1, successCount.get(), "Chỉ duy nhất 1 người dùng được đặt giữ xe thành công");
            assertEquals(totalThreads - 1, conflictCount.get(), "49 người dùng còn lại phải bị từ chối");
            assertEquals("RESERVED", updated.getFleetStatus(), "Trạng thái xe phải là RESERVED");
        } finally {
            // Khôi phục lại trạng thái AVAILABLE cho môi trường demo
            sharedMobilityService.cancelReservation(targetVehicle.getId());
        }
    }
}

