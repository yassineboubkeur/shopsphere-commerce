package com.shopsphere.analytics_service.integration;

import com.shopsphere.analytics_service.entity.AnalyticsEvent;
import com.shopsphere.analytics_service.repository.AnalyticsRepository;
import com.shopsphere.testconfig.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsRepositoryIT extends PostgresTestContainer {

    @MockitoBean
    private com.shopsphere.analytics_service.service.AnalyticsService analyticsService;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @BeforeEach
    void setUp() {
        analyticsRepository.deleteAll();
    }

    @Test
    @DisplayName("save - persists analytics event in PostgreSQL")
    void save_persistsEvent() {
        AnalyticsEvent event = analyticsRepository.save(AnalyticsEvent.builder()
                .eventType("ORDER_CREATED")
                .orderNumber("ORD-AN-001")
                .orderId(101L)
                .userId(4L)
                .productId(1L)
                .quantity(2)
                .amount(new BigDecimal("99.99"))
                .status("PENDING")
                .build());

        assertThat(event.getId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
    }

    @Test
    @DisplayName("findByEventType - returns events of given type")
    void findByEventType_returnsEvents() {
        analyticsRepository.save(AnalyticsEvent.builder()
                .eventType("ORDER_CREATED").orderNumber("ORD-AN-001")
                .userId(4L).amount(new BigDecimal("99.99")).status("PENDING").build());
        analyticsRepository.save(AnalyticsEvent.builder()
                .eventType("PAYMENT_SUCCESSFUL").orderNumber("ORD-AN-001")
                .userId(4L).amount(new BigDecimal("99.99")).status("PAID").build());
        analyticsRepository.save(AnalyticsEvent.builder()
                .eventType("ORDER_CREATED").orderNumber("ORD-AN-002")
                .userId(5L).amount(new BigDecimal("50.00")).status("PENDING").build());

        List<AnalyticsEvent> events = analyticsRepository.findByEventType("ORDER_CREATED");

        assertThat(events).hasSize(2);
        assertThat(events).allMatch(e -> e.getEventType().equals("ORDER_CREATED"));
    }

    @Test
    @DisplayName("sumTotalSales - sums PAYMENT_SUCCESSFUL amounts")
    void sumTotalSales_returnsCorrectSum() {
        analyticsRepository.save(AnalyticsEvent.builder()
                .eventType("PAYMENT_SUCCESSFUL").orderNumber("ORD-1")
                .userId(4L).amount(new BigDecimal("100.00")).status("PAID").build());
        analyticsRepository.save(AnalyticsEvent.builder()
                .eventType("PAYMENT_SUCCESSFUL").orderNumber("ORD-2")
                .userId(5L).amount(new BigDecimal("50.00")).status("PAID").build());

        BigDecimal total = analyticsRepository.sumTotalSales();

        assertThat(total).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("countTotalOrders - counts distinct ORDER_CREATED orders")
    void countTotalOrders_returnsCorrectCount() {
        analyticsRepository.save(AnalyticsEvent.builder()
                .eventType("ORDER_CREATED").orderId(1L).orderNumber("ORD-1")
                .userId(4L).amount(new BigDecimal("50.00")).status("PENDING").build());
        analyticsRepository.save(AnalyticsEvent.builder()
                .eventType("ORDER_CREATED").orderId(2L).orderNumber("ORD-2")
                .userId(5L).amount(new BigDecimal("30.00")).status("PENDING").build());

        long count = analyticsRepository.countTotalOrders();

        assertThat(count).isEqualTo(2);
    }
}
