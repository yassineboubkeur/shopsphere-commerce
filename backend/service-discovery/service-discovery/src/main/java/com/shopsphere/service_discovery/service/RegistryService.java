package com.shopsphere.service_discovery.service;

import com.shopsphere.service_discovery.entity.ServiceInstance;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistryService {

    private final Map<String, ServiceInstance> registry = new ConcurrentHashMap<>();

    public ServiceInstance register(ServiceInstance instance) {
        instance.setRegisteredAt(LocalDateTime.now());
        instance.setLastHeartbeat(LocalDateTime.now());
        registry.put(instance.getName(), instance);
        return instance;
    }

    public boolean renew(String name) {
        ServiceInstance instance = registry.get(name);
        if (instance == null) {
            return false;
        }
        instance.setLastHeartbeat(LocalDateTime.now());
        return true;
    }

    public void deregister(String name) {
        registry.remove(name);
    }

    public List<ServiceInstance> getInstances(String name) {
        ServiceInstance instance = registry.get(name);
        return instance != null ? List.of(instance) : List.of();
    }

    public Map<String, ServiceInstance> getAllInstances() {
        return Map.copyOf(registry);
    }

    public void evictExpired(int expirySeconds) {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(expirySeconds);
        registry.values().removeIf(instance ->
                instance.getLastHeartbeat().isBefore(cutoff));
    }
}