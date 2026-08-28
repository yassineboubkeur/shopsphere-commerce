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

    private static final Map<String, String> ROUTE_TO_SERVICE = Map.of(
            "/api/auth", "auth-service",
            "/api/products", "product-service",
            "/api/orders", "order-service",
            "/api/cart", "order-service",
            "/api/payments", "payment-service",
            "/api/inventory", "inventory-service",
            "/api/notifications", "notification-service",
            "/api/analytics", "analytics-service"
    );

    private static final Map<String, String> FALLBACK_URLS = Map.of(
            "/api/auth", "http://localhost:8081",
            "/api/products", "http://localhost:8082",
            "/api/orders", "http://localhost:8083",
            "/api/cart", "http://localhost:8083",
            "/api/payments", "http://localhost:8084",
            "/api/inventory", "http://localhost:8085",
            "/api/notifications", "http://localhost:8086",
            "/api/analytics", "http://localhost:8087"
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