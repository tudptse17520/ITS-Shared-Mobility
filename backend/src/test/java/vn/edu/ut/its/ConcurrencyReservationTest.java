package vn.edu.ut.its;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.edu.ut.its.dto.mobility.ReserveVehicleRequestDTO;
import vn.edu.ut.its.entity.User;
import vn.edu.ut.its.entity.Vehicle;
import vn.edu.ut.its.exception.ConflictException;
import vn.edu.ut.its.repository.UserRepository;
import vn.edu.ut.its.repository.VehicleRepository;
import vn.edu.ut.its.service.SharedMobilityService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        if ("RESERVED".equalsIgnoreCase(targetVehicle.getFleetStatus())) {
            sharedMobilityService.cancelReservation(targetVehicle.getId());
        }

        int totalThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            attempts.add(() -> {
                readyLatch.countDown();
                readyLatch.await();
                try {
                    sharedMobilityService.reserveVehicle(ReserveVehicleRequestDTO.builder()
                            .vehicleId(targetVehicle.getId())
                            .userId(testUser.getId())
                            .build());
                    return true;
                } catch (ConflictException ex) {
                    return false;
                }
            });
        }

        try {
            int successCount = 0;
            for (Future<Boolean> result : executor.invokeAll(attempts, 30, TimeUnit.SECONDS)) {
                if (result.get()) {
                    successCount++;
                }
            }

            Vehicle updated = vehicleRepository.findById(targetVehicle.getId()).orElseThrow();
            assertEquals(1, successCount, "Chỉ một yêu cầu được đặt giữ xe thành công");
            assertEquals("RESERVED", updated.getFleetStatus(), "Trạng thái xe phải là RESERVED");
        } finally {
            executor.shutdownNow();
            if (vehicleRepository.findById(targetVehicle.getId())
                    .filter(vehicle -> "RESERVED".equals(vehicle.getFleetStatus())).isPresent()) {
                sharedMobilityService.cancelReservation(targetVehicle.getId());
            }
        }
    }
}

