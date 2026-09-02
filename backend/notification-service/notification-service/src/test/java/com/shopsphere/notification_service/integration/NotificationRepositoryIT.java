package com.shopsphere.notification_service.integration;

import com.shopsphere.notification_service.entity.Notification;
import com.shopsphere.notification_service.repository.NotificationRepository;
import com.shopsphere.testconfig.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRepositoryIT extends PostgresTestContainer {

    @MockitoBean
    private com.shopsphere.notification_service.service.NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("save - persists notification in PostgreSQL")
    void save_persistsNotification() {
        Notification notification = notificationRepository.save(Notification.builder()
                .userId(4L)
                .type("ORDER_CONFIRMATION")
                .subject("Order Confirmed")
                .message("Your order ORD-NOT-001 has been confirmed")
                .status("PENDING")
                .build());

        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getSubject()).isEqualTo("Order Confirmed");
        assertThat(notification.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("findByUserId - returns user's notifications")
    void findByUserId_returnsNotifications() {
        notificationRepository.save(Notification.builder()
                .userId(4L).type("ORDER_CONFIRMATION")
                .subject("Order 1").message("msg1").status("SENT").build());
        notificationRepository.save(Notification.builder()
                .userId(4L).type("PAYMENT_RECEIVED")
                .subject("Payment 1").message("msg2").status("SENT").build());
        notificationRepository.save(Notification.builder()
                .userId(5L).type("ORDER_SHIPPED")
                .subject("Order 2").message("msg3").status("PENDING").build());

        List<Notification> notifications = notificationRepository.findByUserId(4L);

        assertThat(notifications).hasSize(2);
        assertThat(notifications).allMatch(n -> n.getUserId().equals(4L));
    }

    @Test
    @DisplayName("findByUserIdAndType - filters by type")
    void findByUserIdAndType_filtersCorrectly() {
        notificationRepository.save(Notification.builder()
                .userId(4L).type("ORDER_CONFIRMATION")
                .subject("Order 1").message("msg1").status("SENT").build());
        notificationRepository.save(Notification.builder()
                .userId(4L).type("PAYMENT_RECEIVED")
                .subject("Payment 1").message("msg2").status("SENT").build());

        List<Notification> filtered = notificationRepository.findByUserIdAndType(4L, "PAYMENT_RECEIVED");

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getType()).isEqualTo("PAYMENT_RECEIVED");
    }
}
