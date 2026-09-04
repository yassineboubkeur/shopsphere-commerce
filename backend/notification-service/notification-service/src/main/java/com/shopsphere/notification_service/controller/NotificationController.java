package com.shopsphere.notification_service.controller;

import com.shopsphere.notification_service.entity.Notification;
import com.shopsphere.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotificationsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.findByUserId(userId));
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<Notification>> getNotificationsByUserAndType(
            @PathVariable Long userId, @PathVariable String type) {
        return ResponseEntity.ok(notificationRepository.findByUserIdAndType(userId, type));
    }

    @GetMapping("/shipping/user/{userId}")
    public ResponseEntity<List<Notification>> getShippingNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdAndType(userId, "SHIPPING_NOTIFICATION"));
    }

    @GetMapping("/order-confirmation/user/{userId}")
    public ResponseEntity<List<Notification>> getOrderConfirmations(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdAndType(userId, "ORDER_CONFIRMATION"));
    }

    @GetMapping("/payment-confirmation/user/{userId}")
    public ResponseEntity<List<Notification>> getPaymentConfirmations(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdAndType(userId, "PAYMENT_CONFIRMATION"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Long id) {
        return notificationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Notification> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return notificationRepository.findById(id)
                .map(n -> {
                    n.setStatus(status.trim().toUpperCase());
                    return notificationRepository.save(n);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
