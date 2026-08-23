package com.shopsphere.analytics_service.repository;

import com.shopsphere.analytics_service.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface AnalyticsRepository extends JpaRepository<AnalyticsEvent, Long> {

    List<AnalyticsEvent> findByEventType(String eventType);

    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM AnalyticsEvent a WHERE a.eventType = 'PAYMENT_SUCCESSFUL'")
    BigDecimal sumTotalSales();

    @Query("SELECT COUNT(DISTINCT a.orderId) FROM AnalyticsEvent a WHERE a.eventType = 'ORDER_CREATED'")
    long countTotalOrders();

    @Query("SELECT a.productName, SUM(a.quantity) as totalQty FROM AnalyticsEvent a WHERE a.eventType = 'ORDER_CREATED' GROUP BY a.productName ORDER BY totalQty DESC")
    List<Object[]> findBestSellingProducts();

    @Query("SELECT FUNCTION('DATE', a.eventTimestamp), SUM(a.amount) FROM AnalyticsEvent a WHERE a.eventType = 'PAYMENT_SUCCESSFUL' GROUP BY FUNCTION('DATE', a.eventTimestamp) ORDER BY FUNCTION('DATE', a.eventTimestamp)")
    List<Object[]> findSalesOverTime();

    @Query("SELECT a.status, COUNT(DISTINCT a.orderId) FROM AnalyticsEvent a WHERE a.eventType = 'ORDER_CREATED' GROUP BY a.status")
    List<Object[]> countOrdersByStatus();
}
