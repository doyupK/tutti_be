package com.tutti.backend.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class EmitterRepository {

    public final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();


    public void save(String id, SseEmitter sseEmitter) {
        emitters.put(id, sseEmitter);
    }

    public void saveEventCache(String id, Object event) {
        eventCache.put(id, event);
    }

    public Map<String, SseEmitter> findAllStartWithById(String id) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().equals(id))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Boolean findById(String id) {
        return emitters.containsKey(id);
    }

    public SseEmitter findEmitterById(String id) {
        return emitters.get(id);
    }


    public Map<String, Object> findAllEventCacheStartWithId(String id) {
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(id))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void deleteAllStartWithId(String id) {
        emitters.forEach(
                (key, emitter) -> {
                    if (key.startsWith(id)) {
                        emitters.remove(key);
                    }
                }
        );
    }

    public void deleteById(String id) {
        emitters.remove(id);
    }

    public void deleteAllById(String id) {
        emitters.forEach(
                (key, data) -> {
                    if (key.startsWith(id)) {
                        emitters.remove(key);
                    }
                }
        );
    }

    public void deleteAllEventCacheStartWithId(String id) {
        eventCache.forEach(
                (key, data) -> {
                    if (key.startsWith(id)) {
                        eventCache.remove(key);
                    }
                }
        );
    }
}
