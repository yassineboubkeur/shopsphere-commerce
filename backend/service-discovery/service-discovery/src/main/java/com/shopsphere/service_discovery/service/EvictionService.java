package com.shopsphere.service_discovery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvictionService {

    private final RegistryService registryService;

    @Value("${registry.eviction.expiry-seconds:90}")
    private int expirySeconds;

    @Scheduled(fixedDelayString = "${registry.eviction.interval-seconds:10}000")
    public void evictExpiredServices() {
        int before = registryService.getAllInstances().size();
        registryService.evictExpired(expirySeconds);
        int after = registryService.getAllInstances().size();
        if (before != after) {
            log.warn("Evicted {} expired service(s) (registry: {} -> {})", before - after, before, after);
        }
    }
}