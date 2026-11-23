package vn.edu.ut.its.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterManager {

    // Map stationId to a list of SSE clients
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> clients = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long stationId) {
        SseEmitter emitter = new SseEmitter(600000L); // 10 minutes timeout
        
        Long key = stationId != null ? stationId : -1L; // -1L for global
        clients.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(e -> removeEmitter(key, emitter));

        return emitter;
    }

    private void removeEmitter(Long stationId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = clients.get(stationId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                clients.remove(stationId);
            }
        }
    }

    public void broadcastToStation(Long stationId, Object eventData) {
        // Broadcast to specific station clients
        broadcastToKey("dock-availability", stationId, eventData);
        // Broadcast to global clients (-1L)
        broadcastToKey("dock-availability", -1L, eventData);
    }

    public void broadcastFleetEvent(Object eventData) {
        broadcastToKey("fleet-telemetry", -1L, eventData);
    }

    private void broadcastToKey(String eventName, Long key, Object eventData) {
        CopyOnWriteArrayList<SseEmitter> list = clients.get(key);
        if (list != null && !list.isEmpty()) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(eventData));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            list.removeAll(deadEmitters);
            if (list.isEmpty()) {
                clients.remove(key);
            }
        }
    }
}
