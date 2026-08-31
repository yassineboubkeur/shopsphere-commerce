package com.shopsphere.api_gateway.service;

import com.shopsphere.api_gateway.config.EurekaProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceDiscoveryService {

    private final RestTemplate restTemplate;
    private final EurekaProperties eurekaProperties;

    private static final Map<String, String> ROUTE_TO_SERVICE = Map.ofEntries(
            Map.entry("/api/auth", "auth-service"),
            Map.entry("/api/user", "auth-service"),
            Map.entry("/api/admin", "auth-service"),
            Map.entry("/api/products", "product-service"),
            Map.entry("/api/categories", "product-service"),
            Map.entry("/api/orders", "order-service"),
            Map.entry("/api/cart", "order-service"),
            Map.entry("/api/payments", "payment-service"),
            Map.entry("/api/inventory", "inventory-service"),
            Map.entry("/api/notifications", "notification-service"),
            Map.entry("/api/analytics", "analytics-service")
    );

    private static final Map<String, String> FALLBACK_URLS = Map.ofEntries(
            Map.entry("/api/auth", "http://localhost:8081"),
            Map.entry("/api/user", "http://localhost:8081"),
            Map.entry("/api/admin", "http://localhost:8081"),
            Map.entry("/api/products", "http://localhost:8082"),
            Map.entry("/api/categories", "http://localhost:8082"),
            Map.entry("/api/orders", "http://localhost:8083"),
            Map.entry("/api/cart", "http://localhost:8083"),
            Map.entry("/api/payments", "http://localhost:8084"),
            Map.entry("/api/inventory", "http://localhost:8085"),
            Map.entry("/api/notifications", "http://localhost:8086"),
            Map.entry("/api/analytics", "http://localhost:8087")
    );

    private volatile Map<String, String> serviceUrls = FALLBACK_URLS;

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${eureka.refresh-interval-seconds:30}000",
            initialDelayString = "${eureka.refresh-interval-seconds:30}000")
    public void refresh() {
        if (!eurekaProperties.isEnabled()) {
            return;
        }
        try {
            Map<String, ServiceInstanceDto> instances = restTemplate.exchange(
                    eurekaProperties.getUrl() + "/instances",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, ServiceInstanceDto>>() {
                    }).getBody();

            if (instances == null || instances.isEmpty()) {
                log.warn("Eureka registry is empty, keeping current service URLs");
                return;
            }

            Map<String, String> urls = new HashMap<>();
            ROUTE_TO_SERVICE.forEach((route, service) -> {
                ServiceInstanceDto instance = instances.get(service);
                urls.put(route, instance != null ? instance.url() : FALLBACK_URLS.get(route));
            });
            serviceUrls = Map.copyOf(urls);
            log.info("Resolved service routes from Eureka: {}", serviceUrls);
        } catch (Exception e) {
            log.warn("Eureka lookup failed ({}), keeping current service URLs", e.getMessage());
        }
    }

    public String resolve(String path) {
        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}