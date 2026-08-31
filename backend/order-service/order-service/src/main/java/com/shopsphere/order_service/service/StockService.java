package com.shopsphere.order_service.service;

import com.shopsphere.order_service.dto.ProductResponse;
import com.shopsphere.order_service.entity.Cart;
import com.shopsphere.order_service.entity.CartItem;
import com.shopsphere.order_service.entity.Order;
import com.shopsphere.order_service.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockService {

    private final RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8082/api/products/";

    public void validateStock(Cart cart) {
        for (CartItem item : cart.getItems()) {
            ProductResponse product = restTemplate.getForObject(
                    PRODUCT_SERVICE_URL + item.getProductId(),
                    ProductResponse.class);

            if (product == null) {
                throw new RuntimeException("Product not found: " + item.getProductId());
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for " + item.getProductName()
                                + " | Available: " + product.getStockQuantity()
                                + " | Requested: " + item.getQuantity());
            }
        }
    }

    public void decrementStock(Order order) {
        for (OrderItem item : order.getItems()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(Collections.singletonMap("quantity", item.getQuantity()), headers);
            restTemplate.exchange(
                    PRODUCT_SERVICE_URL + item.getProductId() + "/stock/decrement",
                    HttpMethod.POST,
                    entity,
                    Void.class);
        }
    }

    public void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(Collections.singletonMap("quantity", item.getQuantity()), headers);
            restTemplate.exchange(
                    PRODUCT_SERVICE_URL + item.getProductId() + "/stock/increase",
                    HttpMethod.POST,
                    entity,
                    Void.class);
        }
    }
}
