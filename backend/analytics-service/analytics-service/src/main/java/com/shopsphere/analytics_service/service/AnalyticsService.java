package com.shopsphere.analytics_service.service;

import com.shopsphere.analytics_service.entity.AnalyticsEvent;
import com.shopsphere.analytics_service.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsEvent save(AnalyticsEvent event) {
        AnalyticsEvent saved = analyticsRepository.save(event);
        log.info("Analytics event saved: id={} | type={} | order={}", saved.getId(), saved.getEventType(), saved.getOrderNumber());
        return saved;
    }

    public List<AnalyticsEvent> saveAll(List<AnalyticsEvent> events) {
        List<AnalyticsEvent> saved = analyticsRepository.saveAll(events);
        log.info("Saved {} analytics events", saved.size());
        return saved;
    }

    public List<AnalyticsEvent> findAll() {
        return analyticsRepository.findAll();
    }

    public List<AnalyticsEvent> findByEventType(String eventType) {
        return analyticsRepository.findByEventType(eventType);
    }

    public BigDecimal getTotalSales() {
        return analyticsRepository.sumTotalSales();
    }

    public long getTotalOrders() {
        return analyticsRepository.countTotalOrders();
    }

    public List<Map<String, Object>> getBestSellingProducts() {
        return analyticsRepository.findBestSellingProducts().stream()
                .map(row -> Map.<String, Object>of("productName", row[0], "totalQuantity", row[1]))
                .toList();
    }

    public List<Map<String, Object>> getSalesOverTime() {
        return analyticsRepository.findSalesOverTime().stream()
                .map(row -> Map.<String, Object>of("date", row[0], "totalSales", row[1]))
                .toList();
    }

    public List<Map<String, Object>> getOrdersByStatus() {
        return analyticsRepository.countOrdersByStatus().stream()
                .map(row -> Map.<String, Object>of("status", row[0], "count", row[1]))
                .toList();
    }
}
