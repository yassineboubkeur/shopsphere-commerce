package com.shopsphere.analytics_service.controller;

import com.shopsphere.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/total-sales")
    public ResponseEntity<Map<String, Object>> getTotalSales() {
        BigDecimal totalSales = analyticsService.getTotalSales();
        return ResponseEntity.ok(Map.of("totalSales", totalSales));
    }

    @GetMapping("/total-orders")
    public ResponseEntity<Map<String, Object>> getTotalOrders() {
        long totalOrders = analyticsService.getTotalOrders();
        return ResponseEntity.ok(Map.of("totalOrders", totalOrders));
    }

    @GetMapping("/best-selling")
    public ResponseEntity<List<Map<String, Object>>> getBestSellingProducts() {
        return ResponseEntity.ok(analyticsService.getBestSellingProducts());
    }

    @GetMapping("/sales-over-time")
    public ResponseEntity<List<Map<String, Object>>> getSalesOverTime() {
        return ResponseEntity.ok(analyticsService.getSalesOverTime());
    }

    @GetMapping("/orders-by-status")
    public ResponseEntity<List<Map<String, Object>>> getOrdersByStatus() {
        return ResponseEntity.ok(analyticsService.getOrdersByStatus());
    }
}
